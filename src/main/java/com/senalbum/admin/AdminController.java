package com.senalbum.admin;

import com.senalbum.album.Album;
import com.senalbum.album.AlbumRepository;
import com.senalbum.payment.PaymentTransaction;
import com.senalbum.payment.PaymentRepository;
import com.senalbum.photo.PhotoRepository;
import com.senalbum.photographer.Photographer;
import com.senalbum.photographer.PhotographerRepository;
import com.senalbum.photographer.SubscriptionPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contrôleur d'administration pour le dashboard SaaS
 * TODO: Ajouter une vérification de rôle ADMIN
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = { "http://localhost:4200", "http://localhost:4201" })
public class AdminController {

  @Autowired
  private PhotographerRepository photographerRepository;

  @Autowired
  private AlbumRepository albumRepository;

  @Autowired
  private PhotoRepository photoRepository;

  @Autowired
  private PaymentRepository paymentRepository;

  /**
   * Statistiques globales de la plateforme
   */
  @GetMapping("/stats")
  public ResponseEntity<AdminStatsDTO> getGlobalStats() {
    long totalUsers = photographerRepository.count();

    List<Photographer> allPhotographers = photographerRepository.findAll();
    long proUsers = allPhotographers.stream()
        .filter(p -> p.getSubscriptionPlan() == SubscriptionPlan.PRO)
        .count();
    long enterpriseUsers = allPhotographers.stream()
        .filter(p -> p.getSubscriptionPlan() == SubscriptionPlan.STUDIO)
        .count();

    long totalAlbums = albumRepository.count();
    long totalPhotos = photoRepository.count();

    // Calculate total views and downloads
    Long totalViews = albumRepository.getGlobalTotalViews();
    Long totalDownloads = albumRepository.getGlobalTotalDownloads();

    // Calculate total storage
    Long totalStorage = photoRepository.getGlobalTotalSize();

    // Calculate monthly revenue from completed transactions this month
    LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
    Double monthlyRevenue = paymentRepository.getRevenueAfterDate(startOfMonth);

    // New users this month
    long newUsersThisMonth = photographerRepository.countByCreatedAtAfter(startOfMonth);

    // New albums this month
    long newAlbumsThisMonth = albumRepository.countByCreatedAtAfter(startOfMonth);

    AdminStatsDTO stats = new AdminStatsDTO(
        totalUsers,
        proUsers,
        enterpriseUsers,
        totalAlbums,
        totalPhotos,
        totalViews != null ? totalViews : 0,
        totalDownloads != null ? totalDownloads : 0,
        totalStorage != null ? totalStorage : 0,
        monthlyRevenue != null ? monthlyRevenue : 0,
        newUsersThisMonth,
        newAlbumsThisMonth);

    return ResponseEntity.ok(stats);
  }

  /**
   * Liste des utilisateurs avec pagination
   */
  @GetMapping("/users")
  public ResponseEntity<List<AdminUserDTO>> getUsers(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String plan,
      @RequestParam(required = false) String search) {

    List<Photographer> photographers = photographerRepository.findAll(
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();

    // Filter by plan if specified
    if (plan != null && !plan.isEmpty()) {
      try {
        SubscriptionPlan targetPlan = SubscriptionPlan.valueOf(plan.toUpperCase());
        photographers = photographers.stream()
            .filter(p -> p.getSubscriptionPlan() == targetPlan)
            .collect(Collectors.toList());
      } catch (IllegalArgumentException ignored) {
      }
    }

    // Filter by search term
    if (search != null && !search.isEmpty()) {
      String searchLower = search.toLowerCase();
      photographers = photographers.stream()
          .filter(p -> (p.getEmail() != null && p.getEmail().toLowerCase().contains(searchLower)) ||
              (p.getFirstName() != null && p.getFirstName().toLowerCase().contains(searchLower)) ||
              (p.getLastName() != null && p.getLastName().toLowerCase().contains(searchLower)))
          .collect(Collectors.toList());
    }

    List<AdminUserDTO> users = photographers.stream()
        .map(this::mapToAdminUserDTO)
        .collect(Collectors.toList());

    return ResponseEntity.ok(users);
  }

  /**
   * Total des utilisateurs (pour la pagination)
   */
  @GetMapping("/users/count")
  public ResponseEntity<Long> getUserCount() {
    return ResponseEntity.ok(photographerRepository.count());
  }

  /**
   * Liste des albums avec pagination
   */
  @GetMapping("/albums")
  public ResponseEntity<List<AdminAlbumDTO>> getAlbums(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    List<Album> albums = albumRepository.findAll(
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();

    List<AdminAlbumDTO> albumDTOs = albums.stream()
        .map(this::mapToAdminAlbumDTO)
        .collect(Collectors.toList());

    return ResponseEntity.ok(albumDTOs);
  }

  /**
   * Liste des transactions avec pagination
   */
  @GetMapping("/transactions")
  public ResponseEntity<List<AdminTransactionDTO>> getTransactions(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    List<PaymentTransaction> transactions = paymentRepository.findAll(
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();

    List<AdminTransactionDTO> transactionDTOs = transactions.stream()
        .map(this::mapToAdminTransactionDTO)
        .collect(Collectors.toList());

    return ResponseEntity.ok(transactionDTOs);
  }

  /**
   * Revenus par période
   */
  @GetMapping("/revenue")
  public ResponseEntity<Double> getRevenue(
      @RequestParam(required = false) String period) {

    LocalDateTime startDate;
    switch (period != null ? period : "month") {
      case "week":
        startDate = LocalDateTime.now().minusWeeks(1);
        break;
      case "year":
        startDate = LocalDateTime.now().minusYears(1);
        break;
      case "all":
        startDate = LocalDateTime.of(2000, 1, 1, 0, 0);
        break;
      case "month":
      default:
        startDate = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        break;
    }

    Double revenue = paymentRepository.getRevenueAfterDate(startDate);
    return ResponseEntity.ok(revenue != null ? revenue : 0.0);
  }

  // ============ HELPER METHODS ============

  private AdminUserDTO mapToAdminUserDTO(Photographer p) {
    long albumCount = albumRepository.countByPhotographer(p);
    Long storageUsed = photoRepository.getTotalSize(p.getId());

    return new AdminUserDTO(
        p.getId(),
        p.getEmail(),
        p.getFirstName(),
        p.getLastName(),
        p.getProfilePictureUrl(),
        p.getSubscriptionPlan() != null ? p.getSubscriptionPlan().name() : "FREE",
        p.getCreatedAt(),
        albumCount,
        storageUsed != null ? storageUsed : 0,
        "active", // TODO: Implement proper status tracking
        p.getCreatedAt() // TODO: Track last activity
    );
  }

  private AdminAlbumDTO mapToAdminAlbumDTO(Album a) {
    String ownerName = "";
    String ownerEmail = "";
    if (a.getPhotographer() != null) {
      Photographer p = a.getPhotographer();
      ownerName = (p.getFirstName() != null ? p.getFirstName() : "") +
          " " + (p.getLastName() != null ? p.getLastName() : "");
      ownerName = ownerName.trim().isEmpty() ? p.getEmail().split("@")[0] : ownerName.trim();
      ownerEmail = p.getEmail();
    }

    int photoCount = a.getPhotos() != null ? a.getPhotos().size() : 0;

    // Calculate size
    long sizeBytes = 0;
    if (a.getPhotos() != null) {
      sizeBytes = a.getPhotos().stream()
          .mapToLong(photo -> photo.getSize() != null ? photo.getSize() : 0)
          .sum();
    }

    // Determine status
    String status = "active";
    if (a.getExpiresAt() != null && a.getExpiresAt().isBefore(LocalDateTime.now())) {
      status = "expired";
    } else if (a.getPasswordHash() != null && !a.getPasswordHash().isEmpty()) {
      status = "protected";
    }

    return new AdminAlbumDTO(
        a.getId(),
        a.getTitle(),
        ownerName,
        ownerEmail,
        a.getCoverImagePath(),
        photoCount,
        a.getViewCount() != null ? a.getViewCount() : 0,
        a.getDownloadCount() != null ? a.getDownloadCount() : 0,
        sizeBytes,
        status,
        a.getCreatedAt(),
        a.getExpiresAt());
  }

  private AdminTransactionDTO mapToAdminTransactionDTO(PaymentTransaction t) {
    String userName = "";
    String userEmail = "";
    if (t.getPhotographer() != null) {
      Photographer p = t.getPhotographer();
      userName = (p.getFirstName() != null ? p.getFirstName() : "") +
          " " + (p.getLastName() != null ? p.getLastName() : "");
      userName = userName.trim().isEmpty() ? p.getEmail().split("@")[0] : userName.trim();
      userEmail = p.getEmail();
    }

    String type = "subscription";
    if (t.getCreditsQuantity() != null && t.getCreditsQuantity() > 0) {
      type = "credits";
    }

    return new AdminTransactionDTO(
        t.getId(),
        userEmail,
        userName,
        type,
        t.getPlanTarget() != null ? t.getPlanTarget().name() : null,
        t.getAmount(),
        t.getStatus(),
        "paydunya", // TODO: Store actual payment method
        t.getCreatedAt());
  }
}

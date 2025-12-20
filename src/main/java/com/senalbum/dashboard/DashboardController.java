package com.senalbum.dashboard;

import com.senalbum.album.AlbumRepository;
import com.senalbum.dashboard.dto.DashboardStatsResponse;
import com.senalbum.photo.PhotoRepository;
import com.senalbum.photographer.Photographer;
import com.senalbum.photographer.PhotographerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

  @Autowired
  private AlbumRepository albumRepository;

  @Autowired
  private PhotoRepository photoRepository;

  @Autowired
  private PhotographerRepository photographerRepository;

  @GetMapping("/albums/{albumId}/stats")
  public AlbumStatsResponse getAlbumStats(@org.springframework.web.bind.annotation.PathVariable java.util.UUID albumId,
      Authentication authentication) {
    String email = authentication.getName();
    Photographer photographer = photographerRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("Photographer not found"));

    com.senalbum.album.Album album = albumRepository.findById(albumId)
        .orElseThrow(() -> new RuntimeException("Album not found"));

    // Vérifier que l'album appartient au photographe
    if (!album.getPhotographer().getId().equals(photographer.getId())) {
      throw new RuntimeException("Unauthorized");
    }

    return new AlbumStatsResponse(
        album.getViewCount() != null ? album.getViewCount() : 0L,
        album.getDownloadCount() != null ? album.getDownloadCount() : 0L);
  }

  @GetMapping("/stats")
  public DashboardStatsResponse getStats(Authentication authentication) {
    String email = authentication.getName();
    Photographer photographer = photographerRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("Photographer not found"));

    long activeAlbums = albumRepository.countByPhotographer(photographer);
    Long views = albumRepository.getTotalViews(photographer.getId());
    Long downloads = albumRepository.getTotalDownloads(photographer.getId());
    Long size = photoRepository.getTotalSize(photographer.getId());

    long maxAlbums;
    long totalStorage;

    com.senalbum.photographer.SubscriptionPlan plan = photographer.getSubscriptionPlan();
    maxAlbums = plan.getMaxAlbums();
    totalStorage = plan.getMaxStorageBytes();

    return new DashboardStatsResponse(
        activeAlbums,
        views != null ? views : 0,
        downloads != null ? downloads : 0,
        size != null ? size : 0,
        totalStorage,
        maxAlbums,
        photographer.getExtraAlbumCredits() != null ? photographer.getExtraAlbumCredits() : 0);
  }
}

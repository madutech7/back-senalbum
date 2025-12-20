package com.senalbum.album;

import com.senalbum.album.dto.AlbumCreateRequest;
import com.senalbum.album.dto.AlbumResponse;
import com.senalbum.photographer.Photographer;
import com.senalbum.photographer.PhotographerRepository;
import com.senalbum.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.senalbum.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.senalbum.album.dto.AlbumUpdateRequest;

/**
 * Service de gestion des albums
 */
@Service
public class AlbumService {

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private PhotographerRepository photographerRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private static final String TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TOKEN_LENGTH = 32;

    /**
     * Génère un token unique pour l'accès public
     */
    private String generateUniqueToken() {
        SecureRandom random = new SecureRandom();
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);

        String generatedToken;
        do {
            token.setLength(0);
            for (int i = 0; i < TOKEN_LENGTH; i++) {
                token.append(TOKEN_CHARS.charAt(random.nextInt(TOKEN_CHARS.length())));
            }
            generatedToken = token.toString();
        } while (albumRepository.existsByToken(generatedToken));

        return generatedToken;
    }

    public AlbumResponse createAlbum(UUID photographerId, AlbumCreateRequest request, MultipartFile coverImage) {
        Photographer photographer = photographerRepository.findById(photographerId)
                .orElseThrow(() -> new RuntimeException("Photographer not found"));

        com.senalbum.photographer.SubscriptionPlan plan = photographer.getSubscriptionPlan();

        // 1. Check Album Limits
        boolean limitReached = false;
        long limit = 0;

        if (plan == com.senalbum.photographer.SubscriptionPlan.FREE) {
            long currentAlbumCount = albumRepository.countByPhotographer(photographer);
            limit = plan.getMaxAlbums();
            if (currentAlbumCount >= limit) {
                limitReached = true;
            }
        } else if (plan == com.senalbum.photographer.SubscriptionPlan.PRO) {
            java.time.LocalDateTime startOfMonth = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();
            long monthlyCount = albumRepository.countByPhotographerAndCreatedAtAfter(photographer, startOfMonth);
            limit = plan.getMaxAlbums();
            if (monthlyCount >= limit) {
                limitReached = true;
            }
        }
        // STUDIO has unlimited albums (maxAlbums = -1), so no check needed.

        if (limitReached) {
            if (photographer.getExtraAlbumCredits() != null && photographer.getExtraAlbumCredits() > 0) {
                photographer.setExtraAlbumCredits(photographer.getExtraAlbumCredits() - 1);
                photographerRepository.save(photographer);
            } else {
                if (plan == com.senalbum.photographer.SubscriptionPlan.FREE) {
                    throw new RuntimeException("Limite atteinte : Le forfait Gratuit est limité à " + limit
                            + " albums. Passez à Pro pour plus !");
                } else {
                    throw new RuntimeException("Limite atteinte : Vous avez atteint votre quota de " + limit
                            + " albums ce mois-ci.");
                }
            }
        }

        Album album = new Album();
        album.setPhotographer(photographer);
        album.setTitle(request.getTitle());
        album.setDescription(request.getDescription());
        album.setToken(generateUniqueToken());

        // 2. Set Expiration / Validity
        if (plan == com.senalbum.photographer.SubscriptionPlan.FREE) {
            // Force 14 days validity for Free plan
            album.setExpiresAt(java.time.LocalDateTime.now().plusDays(plan.getValidityDays()));
        } else {
            // Use requested expiration for Pro/Studio (can be null for unlimited)
            album.setExpiresAt(request.getExpiresAt());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            album.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (coverImage != null && !coverImage.isEmpty()) {
            try {
                // Only allow custom branding (watermark/brand name) if plan supports it
                boolean applyWatermark = Boolean.TRUE.equals(photographer.getWatermarkEnabled())
                        && plan.isCustomBranding();
                String brandName = plan.isCustomBranding() ? photographer.getBrandName() : null;

                String coverPath = storageService.savePreview(
                        coverImage,
                        "covers",
                        applyWatermark,
                        brandName);
                album.setCoverImagePath(coverPath);
            } catch (Exception e) {
                throw new RuntimeException("Failed to save cover image", e);
            }
        }

        album = albumRepository.save(album);

        return toResponse(album);
    }

    public String unlockAlbum(String token, String password) {
        Album album = albumRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Album not found"));

        if (album.getPasswordHash() == null) {
            return jwtUtil.generateAlbumToken(token); // No password needed, provide token anyway
        }

        if (password == null || !passwordEncoder.matches(password, album.getPasswordHash())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        return jwtUtil.generateAlbumToken(token);
    }

    public List<AlbumResponse> getPhotographerAlbums(UUID photographerId) {
        Photographer photographer = photographerRepository.findById(photographerId)
                .orElseThrow(() -> new RuntimeException("Photographer not found"));

        return albumRepository.findByPhotographerOrderByCreatedAtDesc(photographer)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public AlbumResponse getAlbumById(UUID photographerId, UUID albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("Album not found"));

        // Vérifier que l'album appartient au photographe
        if (!album.getPhotographer().getId().equals(photographerId)) {
            throw new RuntimeException("Unauthorized: Album does not belong to photographer");
        }

        return toResponse(album);
    }

    public AlbumResponse getAlbumByToken(String token) {
        Album album = albumRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Album not found"));

        // Vérifier si l'album n'est pas expiré
        if (album.getExpiresAt() != null && album.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Album has expired");
        }

        return toResponse(album);
    }

    public AlbumResponse getAlbumByStudioAndTitle(String studioName, String title) {
        Album album = albumRepository.findByBrandNameAndTitle(studioName, title)
                .orElseThrow(() -> new RuntimeException("Album not found for this studio and title"));

        if (album.getExpiresAt() != null && album.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Album has expired");
        }

        // Restriction: Custom URLs are only for STUDIO plan
        if (album.getPhotographer().getSubscriptionPlan() != com.senalbum.photographer.SubscriptionPlan.STUDIO) {
            throw new RuntimeException("This feature is reserved for Studio plans.");
        }

        return toResponse(album);
    }

    @Transactional
    public void deleteAlbum(UUID photographerId, UUID albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("Album not found"));

        // Vérifier que l'album appartient au photographe
        if (!album.getPhotographer().getId().equals(photographerId)) {
            throw new RuntimeException("Unauthorized: Album does not belong to photographer");
        }

        // Supprimer les fichiers physiques des photos
        if (album.getPhotos() != null) {
            System.out.println("DEBUG: Deleting " + album.getPhotos().size() + " photos for album " + albumId);
            for (com.senalbum.photo.Photo photo : album.getPhotos()) {
                try {
                    if (photo.getOriginalPath() != null) {
                        System.out.println("DEBUG: Deleting original file: " + photo.getOriginalPath());
                        storageService.deleteFile(photo.getOriginalPath());
                    }
                    if (photo.getPreviewPath() != null) {
                        System.out.println("DEBUG: Deleting preview file: " + photo.getPreviewPath());
                        storageService.deleteFile(photo.getPreviewPath());
                    }
                } catch (Exception e) {
                    // Log warning but continue
                    System.err.println(
                            "Warning: Failed to delete files for photo " + photo.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("DEBUG: No photos found to delete for album " + albumId);
        }

        // Supprimer l'image de couverture si elle existe
        if (album.getCoverImagePath() != null) {
            try {
                System.out.println("DEBUG: Deleting cover image: " + album.getCoverImagePath());
                storageService.deleteFile(album.getCoverImagePath());
            } catch (Exception e) {
                System.err.println("Warning: Failed to delete album cover: " + e.getMessage());
                e.printStackTrace();
            }
        }

        albumRepository.delete(album);
    }

    public Album getAlbumEntity(UUID albumId) {
        return albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("Album not found"));
    }

    public Album getAlbumEntityByToken(String token) {
        Album album = albumRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Album not found"));

        if (album.getExpiresAt() != null && album.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Album has expired");
        }

        return album;
    }

    public byte[] getCoverImage(String token) {
        Album album = getAlbumEntityByToken(token);
        if (album.getCoverImagePath() == null) {
            throw new RuntimeException("Cover image not found");
        }
        try {
            return storageService.getPreviewFile(album.getCoverImagePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get cover image", e);
        }
    }

    @Transactional
    public void incrementViewCount(String token) {
        Album album = albumRepository.findByToken(token).orElse(null);
        if (album != null) {
            Long current = album.getViewCount() != null ? album.getViewCount() : 0L;
            album.setViewCount(current + 1);
            albumRepository.save(album);
        }
    }

    @Transactional
    public void incrementDownloadCount(String token) {
        Album album = albumRepository.findByToken(token).orElse(null);
        if (album != null) {
            Long current = album.getDownloadCount() != null ? album.getDownloadCount() : 0L;
            album.setDownloadCount(current + 1);
            albumRepository.save(album);
        }
    }

    @Transactional
    public AlbumResponse updateAlbum(UUID photographerId, UUID albumId,
            AlbumUpdateRequest request, MultipartFile coverImage) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("Album not found"));

        if (!album.getPhotographer().getId().equals(photographerId)) {
            throw new RuntimeException("Unauthorized: Album does not belong to photographer");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            album.setTitle(request.getTitle());
        }

        // Description can be cleared if empty string is passed, or updated
        if (request.getDescription() != null) {
            album.setDescription(request.getDescription());
        }

        if (request.getExpiresAt() != null) {
            album.setExpiresAt(request.getExpiresAt());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            album.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        } else if (request.getPassword() != null && request.getPassword().isEmpty()) {
            // Option to remove password if empty string starts being passed to mean
            // "remove"
            // For now, let's assume we don't clear it unless explicit (maybe need a
            // separate flag or specific string?)
            // Or simpler: if empty string is passed, we clear the password.
            album.setPasswordHash(null);
        }

        if (coverImage != null && !coverImage.isEmpty()) {
            try {
                // Delete old cover if exists
                if (album.getCoverImagePath() != null) {
                    try {
                        storageService.deleteFile(album.getCoverImagePath());
                    } catch (Exception e) {
                        System.err.println("Warning: Failed to delete old cover image: " + e.getMessage());
                    }
                }

                String coverPath = storageService.savePreview(
                        coverImage,
                        "covers",
                        Boolean.TRUE.equals(album.getPhotographer().getWatermarkEnabled()),
                        album.getPhotographer().getBrandName());
                album.setCoverImagePath(coverPath);
            } catch (Exception e) {
                throw new RuntimeException("Failed to save new cover image", e);
            }
        }

        album = albumRepository.save(album);
        return toResponse(album);
    }

    private AlbumResponse toResponse(Album album) {
        String coverUrl = null;
        if (album.getCoverImagePath() != null) {
            try {
                coverUrl = storageService.generatePresignedDownloadUrl(album.getCoverImagePath());
            } catch (Exception e) {
                // Fallback or log if generation fails (e.g. old local storage)
                // If using DatabaseStorage, this throws exception. We can fallback to local
                // URL?
                // For now, let's assume we want Wasabi URLs.
                // If we want to support both, we'd check if path looks like UUID or S3 Key.
                // Assuming switch to Wasabi:
                // coverUrl = null; // or keep existing logic as fallback
            }
            if (coverUrl == null) {
                coverUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/public/albums/" + album.getToken() + "/cover").toUriString();
            }
        }

        Photographer p = album.getPhotographer();
        String brandName = null;
        String brandLogoUrl = null;
        String brandCoverUrl = null;
        String brandPrimaryColor = null;

        if (p.getSubscriptionPlan() != com.senalbum.photographer.SubscriptionPlan.FREE) {
            brandName = p.getBrandName();
            brandLogoUrl = p.getBrandLogoUrl();
            brandCoverUrl = p.getBrandCoverUrl();
            brandPrimaryColor = p.getBrandPrimaryColor();
        }

        return new AlbumResponse(
                album.getId(),
                album.getTitle(),
                album.getDescription(),
                album.getToken(),
                album.getExpiresAt(),
                album.getCreatedAt(),
                coverUrl,
                album.getPasswordHash() != null && !album.getPasswordHash().isBlank(),
                brandName,
                brandLogoUrl,
                brandCoverUrl,
                brandPrimaryColor,
                album.getViewCount() != null ? album.getViewCount() : 0L,
                album.getDownloadCount() != null ? album.getDownloadCount() : 0L);
    }
}

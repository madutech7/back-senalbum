package com.senalbum.photo;

import com.senalbum.album.Album;
import com.senalbum.album.AlbumService;
import com.senalbum.photo.dto.PhotoDownloadDTO;
import com.senalbum.photo.dto.PhotoResponse;
import com.senalbum.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service de gestion des photos
 */
@Service
public class PhotoService {

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private AlbumService albumService;

    @Autowired
    private StorageService storageService;

    @Transactional
    public PhotoResponse uploadPhoto(UUID photographerId, UUID albumId, MultipartFile file) {
        // Vérifier que l'album appartient au photographe
        Album album = albumService.getAlbumEntity(albumId);
        if (!album.getPhotographer().getId().equals(photographerId)) {
            throw new RuntimeException("Unauthorized: Album does not belong to photographer");
        }

        long currentSize = photoRepository.getTotalSize(photographerId) != null
                ? photoRepository.getTotalSize(photographerId)
                : 0L;

        com.senalbum.photographer.SubscriptionPlan plan = album.getPhotographer().getSubscriptionPlan();
        long storageLimit = plan.getMaxStorageBytes();

        if (currentSize + file.getSize() > storageLimit) {
            String limitLabel = (storageLimit / (1024 * 1024)) + " Mo";
            if (storageLimit >= 1024 * 1024 * 1024) {
                limitLabel = (storageLimit / (1024 * 1024 * 1024)) + " Go";
            }
            throw new RuntimeException(
                    "Espace insuffisant : Votre limite de stockage (" + limitLabel + ") est atteinte.");
        }

        try {
            // Sauvegarder les fichiers
            String originalPath = storageService.saveOriginal(file, albumId.toString());
            boolean applyWatermark = Boolean.TRUE.equals(album.getPhotographer().getWatermarkEnabled())
                    && plan.isCustomBranding();
            String brandName = plan.isCustomBranding() ? album.getPhotographer().getBrandName() : null;

            String previewPath = storageService.savePreview(
                    file,
                    albumId.toString(),
                    applyWatermark,
                    brandName);

            // Créer l'entité Photo
            Photo photo = new Photo();
            photo.setAlbum(album);
            photo.setOriginalPath(originalPath);
            photo.setOriginalFilename(file.getOriginalFilename());
            photo.setPreviewPath(previewPath);
            photo.setSize(file.getSize());

            photo = photoRepository.save(photo);

            return toResponse(photo, album.getToken());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload photo: " + e.getMessage(), e);
        }
    }

    @Transactional
    public PhotoResponse confirmUpload(UUID photographerId, UUID albumId,
            com.senalbum.photo.dto.ConfirmUploadRequest request) {
        Album album = albumService.getAlbumEntity(albumId);
        if (!album.getPhotographer().getId().equals(photographerId)) {
            throw new RuntimeException("Unauthorized: Album does not belong to photographer");
        }

        long currentSize = photoRepository.getTotalSize(photographerId) != null
                ? photoRepository.getTotalSize(photographerId)
                : 0L;

        com.senalbum.photographer.SubscriptionPlan plan = album.getPhotographer().getSubscriptionPlan();
        long storageLimit = plan.getMaxStorageBytes();

        if (currentSize + request.getSize() > storageLimit) {
            String limitLabel = (storageLimit / (1024 * 1024)) + " Mo";
            if (storageLimit >= 1024 * 1024 * 1024) {
                limitLabel = (storageLimit / (1024 * 1024 * 1024)) + " Go";
            }
            throw new RuntimeException(
                    "Espace insuffisant : Votre limite de stockage (" + limitLabel + ") est atteinte.");
        }

        Photo photo = new Photo();
        photo.setAlbum(album);
        photo.setOriginalPath(request.getOriginalKey());
        photo.setOriginalFilename(request.getFilename());
        // If previewKey is null, fallback to originalKey (assuming frontend verified it
        // or client handles large images)
        photo.setPreviewPath(request.getPreviewKey() != null ? request.getPreviewKey() : request.getOriginalKey());
        photo.setSize(request.getSize());

        photo = photoRepository.save(photo);

        return toOwnerResponse(photo);
    }

    public List<PhotoResponse> getAlbumPhotos(String albumToken) {
        Album album = albumService.getAlbumEntityByToken(albumToken);
        System.out.println("DEBUG: Fetching photos for token: " + albumToken);
        System.out.println("DEBUG: Resolved Album ID: " + album.getId());

        List<Photo> photos = photoRepository.findByAlbumOrderByCreatedAtAsc(album);
        System.out.println("DEBUG: Found " + photos.size() + " photos in DB for album " + album.getId());

        return photos.stream()
                .map(photo -> toResponse(photo, albumToken))
                .collect(Collectors.toList());
    }

    public byte[] getPreviewPhoto(String albumToken, UUID photoId) {
        Album album = albumService.getAlbumEntityByToken(albumToken);
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));

        if (!photo.getAlbum().getId().equals(album.getId())) {
            throw new RuntimeException("Photo does not belong to album");
        }

        try {
            return storageService.getPreviewFile(photo.getPreviewPath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get preview photo", e);
        }
    }

    public PhotoDownloadDTO getOriginalPhoto(String albumToken, UUID photoId) {
        Album album = albumService.getAlbumEntityByToken(albumToken);
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));

        if (!photo.getAlbum().getId().equals(album.getId())) {
            throw new RuntimeException("Photo does not belong to album");
        }

        try {
            byte[] data = storageService.getOriginalFile(photo.getOriginalPath());
            String filename = photo.getOriginalFilename() != null ? photo.getOriginalFilename()
                    : "photo-" + photo.getId() + ".jpg";
            return new PhotoDownloadDTO(data, filename);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get original photo", e);
        }
    }

    @Transactional
    public void deletePhoto(UUID photographerId, UUID albumId, UUID photoId) {
        Album album = albumService.getAlbumEntity(albumId);

        // Security check: Album must belong to the authenticated photographer
        if (!album.getPhotographer().getId().equals(photographerId)) {
            throw new RuntimeException("Unauthorized: Album does not belong to photographer");
        }

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));

        if (!photo.getAlbum().getId().equals(albumId)) {
            throw new RuntimeException("Photo does not belong to the specified album");
        }

        // Delete files from storage
        try {
            storageService.deleteFile(photo.getOriginalPath());
            storageService.deleteFile(photo.getPreviewPath());
        } catch (Exception e) {
            // Log warning but continue to delete entity
            System.err.println("Warning: Failed to delete files for photo " + photoId + ": " + e.getMessage());
        }

        photoRepository.delete(photo);
    }

    public List<PhotoResponse> getPhotosForOwner(UUID photographerId, UUID albumId) {
        Album album = albumService.getAlbumEntity(albumId);
        if (!album.getPhotographer().getId().equals(photographerId)) {
            throw new RuntimeException("Unauthorized: Album does not belong to photographer");
        }
        return photoRepository.findByAlbumOrderByCreatedAtAsc(album)
                .stream()
                .map(photo -> toOwnerResponse(photo))
                .collect(Collectors.toList());
    }

    public byte[] getPreviewForOwner(UUID photographerId, UUID albumId, UUID photoId) {
        Album album = albumService.getAlbumEntity(albumId);
        if (!album.getPhotographer().getId().equals(photographerId)) {
            throw new RuntimeException("Unauthorized: Album does not belong to photographer");
        }
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));

        if (!photo.getAlbum().getId().equals(albumId)) {
            throw new RuntimeException("Photo does not belong to album");
        }

        try {
            return storageService.getPreviewFile(photo.getPreviewPath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get preview photo", e);
        }
    }

    public PhotoDownloadDTO getOriginalForOwner(UUID photographerId, UUID albumId, UUID photoId) {
        Album album = albumService.getAlbumEntity(albumId);
        if (!album.getPhotographer().getId().equals(photographerId)) {
            throw new RuntimeException("Unauthorized: Album does not belong to photographer");
        }
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));

        if (!photo.getAlbum().getId().equals(albumId)) {
            throw new RuntimeException("Photo does not belong to album");
        }

        try {
            byte[] data = storageService.getOriginalFile(photo.getOriginalPath());
            String filename = photo.getOriginalFilename() != null ? photo.getOriginalFilename()
                    : "photo-" + photo.getId() + ".jpg";
            return new PhotoDownloadDTO(data, filename);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get original photo", e);
        }
    }

    private PhotoResponse toOwnerResponse(Photo photo) {
        String previewUrl;
        String downloadUrl;
        try {
            previewUrl = storageService.generatePresignedDownloadUrl(photo.getPreviewPath());
            downloadUrl = storageService.generatePresignedDownloadUrl(photo.getOriginalPath());
        } catch (Exception e) {
            // Fallback for legacy local files or DB storage
            previewUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/albums/" + photo.getAlbum().getId() + "/photos/" + photo.getId() + "/preview")
                    .toUriString();
            downloadUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/albums/" + photo.getAlbum().getId() + "/photos/" + photo.getId() + "/download")
                    .toUriString();
        }

        return new PhotoResponse(
                photo.getId(),
                previewUrl,
                downloadUrl,
                photo.getCreatedAt());
    }

    private PhotoResponse toResponse(Photo photo, String albumToken) {
        String previewUrl;
        String downloadUrl;
        try {
            previewUrl = storageService.generatePresignedDownloadUrl(photo.getPreviewPath());
            downloadUrl = storageService.generatePresignedDownloadUrl(photo.getOriginalPath());
        } catch (Exception e) {
            previewUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/public/albums/" + albumToken + "/photos/" + photo.getId() + "/preview")
                    .toUriString();
            downloadUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/public/albums/" + albumToken + "/photos/" + photo.getId() + "/download")
                    .toUriString();
        }

        return new PhotoResponse(
                photo.getId(),
                previewUrl,
                downloadUrl,
                photo.getCreatedAt());
    }
}

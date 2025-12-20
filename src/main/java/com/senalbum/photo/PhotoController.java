package com.senalbum.photo;

import com.senalbum.photo.dto.PhotoDownloadDTO;
import com.senalbum.photo.dto.PhotoResponse;
import com.senalbum.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Controller de gestion des photos (endpoints privés)
 */
@RestController
@RequestMapping("/api/albums/{albumId}/photos")
@CrossOrigin(origins = "http://localhost:4200")
public class PhotoController {

    @Autowired
    private PhotoService photoService;

    @Autowired
    private SecurityUtils securityUtils;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoResponse> uploadPhoto(
            @PathVariable UUID albumId,
            @RequestParam("file") MultipartFile file) {
        UUID photographerId = securityUtils.getCurrentPhotographerId();
        PhotoResponse response = photoService.uploadPhoto(photographerId, albumId, file);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<PhotoResponse> confirmUpload(
            @PathVariable UUID albumId,
            @RequestBody com.senalbum.photo.dto.ConfirmUploadRequest request) {
        UUID photographerId = securityUtils.getCurrentPhotographerId();
        PhotoResponse response = photoService.confirmUpload(photographerId, albumId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PhotoResponse>> getAlbumPhotos(@PathVariable UUID albumId) {
        UUID photographerId = securityUtils.getCurrentPhotographerId();
        List<PhotoResponse> photos = photoService.getPhotosForOwner(photographerId, albumId);
        return ResponseEntity.ok(photos);
    }

    @GetMapping("/{photoId}/preview")
    public ResponseEntity<byte[]> getPreviewPhoto(@PathVariable UUID albumId, @PathVariable UUID photoId) {
        UUID photographerId = securityUtils.getCurrentPhotographerId();
        byte[] imageBytes = photoService.getPreviewForOwner(photographerId, albumId, photoId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setContentLength(imageBytes.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400, mutable")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                .contentLength(imageBytes.length)
                .body(imageBytes);
    }

    @GetMapping("/{photoId}/download")
    public ResponseEntity<byte[]> downloadPhoto(@PathVariable UUID albumId, @PathVariable UUID photoId) {
        UUID photographerId = securityUtils.getCurrentPhotographerId();
        PhotoDownloadDTO downloadDTO = photoService.getOriginalForOwner(photographerId, albumId, photoId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", downloadDTO.getFilename());
        headers.setContentLength(downloadDTO.getData().length);

        return new ResponseEntity<>(downloadDTO.getData(), headers, HttpStatus.OK);
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> deletePhoto(@PathVariable UUID albumId, @PathVariable UUID photoId) {
        UUID photographerId = securityUtils.getCurrentPhotographerId();
        photoService.deletePhoto(photographerId, albumId, photoId);
        return ResponseEntity.noContent().build();
    }
}

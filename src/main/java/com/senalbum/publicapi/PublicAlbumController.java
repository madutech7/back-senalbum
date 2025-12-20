package com.senalbum.publicapi;

import com.senalbum.album.AlbumService;
import com.senalbum.album.dto.AlbumResponse;
import com.senalbum.photo.PhotoService;
import com.senalbum.photo.dto.PhotoDownloadDTO;
import com.senalbum.photo.dto.PhotoResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import com.senalbum.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Controller pour l'accès public aux albums (sans authentification)
 */
@RestController
@RequestMapping("/api/public/albums")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class PublicAlbumController {

    @Autowired
    private AlbumService albumService;

    @Autowired
    private PhotoService photoService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/{token}")
    public ResponseEntity<AlbumResponse> getAlbumByToken(@PathVariable String token) {
        AlbumResponse album = albumService.getAlbumByToken(token);
        // Async call (idéalement) mais ici synchrone pour la simplicité
        albumService.incrementViewCount(token);
        return ResponseEntity.ok(album);
    }

    @GetMapping("/{studio}/{title}")
    public ResponseEntity<AlbumResponse> getAlbumByCustomUrl(
            @PathVariable String studio,
            @PathVariable String title) {
        // Simple URL decode might be needed if spaces come in as encoded strings,
        // but Spring Boot usually handles basic path variable decoding.
        AlbumResponse album = albumService.getAlbumByStudioAndTitle(studio, title);
        albumService.incrementViewCount(album.getToken()); // Use token internally for tracking
        return ResponseEntity.ok(album);
    }

    @PostMapping("/{token}/unlock")
    public ResponseEntity<Void> unlockAlbum(
            @PathVariable String token,
            @RequestBody Map<String, String> body,
            HttpServletResponse response) {
        String password = body.get("password");
        String accessToken = albumService.unlockAlbum(token, password);

        // Set Cookie
        Cookie cookie = new Cookie("album_auth_" + token, accessToken);
        cookie.setPath("/"); // Or set to specific path
        cookie.setHttpOnly(true);
        cookie.setMaxAge(60); // 1 minute
        // cookie.setSecure(true); // Enable in production with HTTPS

        response.addCookie(cookie);

        return ResponseEntity.ok().build();
    }

    private void checkAccess(String token, HttpServletRequest request) {
        AlbumResponse album = albumService.getAlbumByToken(token);
        if (!album.isPasswordProtected())
            return;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (c.getName().equals("album_auth_" + token)) {
                    if (jwtUtil.validateAlbumToken(c.getValue(), token)) {
                        return; // Authorized
                    }
                }
            }
        }
        throw new RuntimeException("Unauthorized: Password required");
    }

    @GetMapping("/{token}/photos")
    public ResponseEntity<List<PhotoResponse>> getAlbumPhotos(
            @PathVariable String token,
            HttpServletRequest request) {
        checkAccess(token, request);
        List<PhotoResponse> photos = photoService.getAlbumPhotos(token);
        return ResponseEntity.ok(photos);
    }

    @GetMapping("/{token}/cover")
    public ResponseEntity<byte[]> getCoverImage(@PathVariable String token) {
        // Cover is usually public
        byte[] imageBytes = albumService.getCoverImage(token);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                .contentLength(imageBytes.length)
                .body(imageBytes);
    }

    @GetMapping("/{token}/photos/{photoId}/preview")
    public ResponseEntity<byte[]> getPreviewPhoto(
            @PathVariable String token,
            @PathVariable UUID photoId,
            HttpServletRequest request) {
        checkAccess(token, request);
        byte[] imageBytes = photoService.getPreviewPhoto(token, photoId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                .contentLength(imageBytes.length)
                .body(imageBytes);
    }

    @GetMapping("/{token}/photos/{photoId}/download")
    public ResponseEntity<byte[]> downloadOriginalPhoto(
            @PathVariable String token,
            @PathVariable UUID photoId,
            HttpServletRequest request) {
        checkAccess(token, request);
        PhotoDownloadDTO downloadDTO = photoService.getOriginalPhoto(token, photoId);

        albumService.incrementDownloadCount(token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", downloadDTO.getFilename());
        headers.setContentLength(downloadDTO.getData().length);

        return new ResponseEntity<>(downloadDTO.getData(), headers, HttpStatus.OK);
    }
}

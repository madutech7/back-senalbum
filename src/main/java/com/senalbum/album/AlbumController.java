package com.senalbum.album;

import com.senalbum.album.dto.AlbumCreateRequest;
import com.senalbum.album.dto.AlbumResponse;
import com.senalbum.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;

/**
 * Controller de gestion des albums (endpoints privés)
 */
@RestController
@RequestMapping("/api/albums")
public class AlbumController {

    @Autowired
    private AlbumService albumService;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<AlbumResponse> createAlbum(
            @RequestParam("data") String dataJson,
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage) throws Exception {
        AlbumCreateRequest createRequest = objectMapper.readValue(dataJson, AlbumCreateRequest.class);
        UUID photographerId = securityUtils.getCurrentPhotographerId();
        AlbumResponse response = albumService.createAlbum(photographerId, createRequest, coverImage);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AlbumResponse>> getMyAlbums() {
        UUID photographerId = securityUtils.getCurrentPhotographerId();
        List<AlbumResponse> albums = albumService.getPhotographerAlbums(photographerId);
        return ResponseEntity.ok(albums);
    }

    @GetMapping("/{albumId}")
    public ResponseEntity<AlbumResponse> getAlbumById(@PathVariable UUID albumId) {
        UUID photographerId = securityUtils.getCurrentPhotographerId();
        AlbumResponse album = albumService.getAlbumById(photographerId, albumId);
        return ResponseEntity.ok(album);
    }

    @PutMapping(value = "/{albumId}", consumes = { "multipart/form-data" })
    public ResponseEntity<AlbumResponse> updateAlbum(
            @PathVariable UUID albumId,
            @RequestParam("data") String dataJson,
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage) throws Exception {
        com.senalbum.album.dto.AlbumUpdateRequest updateRequest = objectMapper.readValue(dataJson,
                com.senalbum.album.dto.AlbumUpdateRequest.class);
        UUID photographerId = securityUtils.getCurrentPhotographerId();
        AlbumResponse response = albumService.updateAlbum(photographerId, albumId, updateRequest, coverImage);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{albumId}")
    public ResponseEntity<Void> deleteAlbum(@PathVariable UUID albumId) {
        UUID photographerId = securityUtils.getCurrentPhotographerId();
        albumService.deleteAlbum(photographerId, albumId);
        return ResponseEntity.noContent().build();
    }
}

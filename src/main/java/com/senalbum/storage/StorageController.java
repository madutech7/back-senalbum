package com.senalbum.storage;

import com.senalbum.storage.dto.PresignedUploadRequest;
import com.senalbum.storage.dto.PresignedUploadResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/storage")
public class StorageController {

  @Autowired
  private StorageService storageService;

  @PostMapping("/upload-url")
  public ResponseEntity<PresignedUploadResponse> getUploadUrl(@RequestBody PresignedUploadRequest request) {
    String folder = request.getAlbumId() != null ? "albums/" + request.getAlbumId() : "uploads";
    String key = folder + "/" + UUID.randomUUID() + "-" + request.getFilename();

    String url = storageService.generatePresignedUploadUrl(key, request.getContentType());

    return ResponseEntity.ok(new PresignedUploadResponse(url, key));
  }
}

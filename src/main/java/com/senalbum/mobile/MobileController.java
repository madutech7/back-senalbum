package com.senalbum.mobile;

import com.senalbum.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MobileController {

  @Autowired
  private MobileReleaseRepository releaseRepository;

  @Autowired
  private StorageService storageService;

  // Public endpoint to get the latest release and its download URL
  @GetMapping("/public/mobile/latest")
  public ResponseEntity<?> getLatestRelease() {
    return releaseRepository.findFirstByOrderByCreatedAtDesc()
        .map(release -> {
          String downloadUrl = storageService.generatePresignedDownloadUrl(release.getWasabiKey());
          return ResponseEntity.ok(Map.of(
              "id", release.getId(),
              "version", release.getVersion(),
              "changelog", release.getChangelog(),
              "downloadUrl", downloadUrl,
              "createdAt", release.getCreatedAt()));
        })
        .orElse(ResponseEntity.notFound().build());
  }

  // Dashboard endpoint to register a new release
  @PostMapping("/mobile/releases")
  public ResponseEntity<MobileRelease> createRelease(@RequestBody MobileRelease release) {
    MobileRelease saved = releaseRepository.save(release);
    if (saved == null) {
      return ResponseEntity.internalServerError().build();
    }
    return ResponseEntity.ok(saved);
  }

  @GetMapping("/mobile/releases")
  public ResponseEntity<List<MobileRelease>> getAllReleases() {
    return ResponseEntity.ok(releaseRepository.findAllByOrderByCreatedAtDesc());
  }
}

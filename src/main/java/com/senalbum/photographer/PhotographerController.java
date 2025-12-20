package com.senalbum.photographer;

import com.senalbum.photographer.dto.UpdateProfileRequest;
import com.senalbum.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.senalbum.storage.StorageService;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.io.IOException;

import java.util.UUID;

@RestController
@RequestMapping("/api/photographer")
public class PhotographerController {

  @Autowired
  private PhotographerRepository photographerRepository;

  @Autowired
  private SecurityUtils securityUtils;

  @Autowired
  private StorageService storageService;

  @GetMapping("/me")
  public ResponseEntity<Photographer> getCurrentPhotographer() {
    UUID id = securityUtils.getCurrentPhotographerId();
    Photographer photographer = photographerRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Photographer not found"));
    return ResponseEntity.ok(photographer);
  }

  @PutMapping("/profile")
  public ResponseEntity<Photographer> updateProfile(@RequestBody UpdateProfileRequest request) {
    UUID id = securityUtils.getCurrentPhotographerId();
    Photographer photographer = photographerRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Photographer not found"));

    if (request.getFirstName() != null)
      photographer.setFirstName(request.getFirstName());
    if (request.getLastName() != null)
      photographer.setLastName(request.getLastName());

    // Only update branding if user is paid
    if (photographer.getSubscriptionPlan() != SubscriptionPlan.FREE) {
      if (request.getBrandName() != null)
        photographer.setBrandName(request.getBrandName());
      if (request.getBrandLogoUrl() != null)
        photographer.setBrandLogoUrl(request.getBrandLogoUrl());
      if (request.getBrandCoverUrl() != null)
        photographer.setBrandCoverUrl(request.getBrandCoverUrl());
      if (request.getBrandPrimaryColor() != null)
        photographer.setBrandPrimaryColor(request.getBrandPrimaryColor());
      if (request.getWatermarkEnabled() != null)
        photographer.setWatermarkEnabled(request.getWatermarkEnabled());

      // Custom domain only for STUDIO
      if (photographer.getSubscriptionPlan() == SubscriptionPlan.STUDIO) {
        if (request.getCustomDomain() != null)
          photographer.setCustomDomain(request.getCustomDomain());
      }
    }

    if (request.getNotifyDownloads() != null)
      photographer.setNotifyDownloads(request.getNotifyDownloads());
    if (request.getNotifyViews() != null)
      photographer.setNotifyViews(request.getNotifyViews());

    if (request.getProfilePictureUrl() != null)
      photographer.setProfilePictureUrl(request.getProfilePictureUrl());

    Photographer updated = photographerRepository.save(photographer);
    return ResponseEntity.ok(updated);
  }

  @PostMapping(value = "/logo", consumes = "multipart/form-data")
  public ResponseEntity<Photographer> uploadLogo(@RequestParam("file") MultipartFile file) throws IOException {
    UUID id = securityUtils.getCurrentPhotographerId();
    Photographer photographer = photographerRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Photographer not found"));

    if (photographer.getSubscriptionPlan() == SubscriptionPlan.FREE) {
      throw new RuntimeException("Branding is a paid feature");
    }

    // Save file using StorageService (assuming DatabaseStorageService is active)
    String fileId = storageService.saveOriginal(file, "branding");

    // Construct Public URL
    String logoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
        .path("/api/public/files/")
        .path(fileId)
        .toUriString();

    photographer.setBrandLogoUrl(logoUrl);
    Photographer updated = photographerRepository.save(photographer);

    return ResponseEntity.ok(updated);
  }

  @PostMapping(value = "/cover", consumes = "multipart/form-data")
  public ResponseEntity<Photographer> uploadCover(@RequestParam("file") MultipartFile file) throws IOException {
    UUID id = securityUtils.getCurrentPhotographerId();
    Photographer photographer = photographerRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Photographer not found"));

    if (photographer.getSubscriptionPlan() == SubscriptionPlan.FREE) {
      throw new RuntimeException("Branding is a paid feature");
    }

    String fileId = storageService.saveOriginal(file, "branding");

    String coverUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
        .path("/api/public/files/")
        .path(fileId)
        .toUriString();

    photographer.setBrandCoverUrl(coverUrl);
    Photographer updated = photographerRepository.save(photographer);

    return ResponseEntity.ok(updated);
  }
}

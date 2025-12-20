package com.senalbum.publicapi;

import com.senalbum.storage.FileContent;
import com.senalbum.storage.FileContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/files")
@CrossOrigin(origins = "http://localhost:4200")
public class PublicFileController {

  @Autowired
  private com.senalbum.storage.StorageService storageService;

  @GetMapping("/**")
  public ResponseEntity<byte[]> getFile(jakarta.servlet.http.HttpServletRequest request) throws java.io.IOException {
    String requestURI = request.getRequestURI();
    String prefix = "/api/public/files/";
    int index = requestURI.indexOf(prefix);

    if (index == -1) {
      return ResponseEntity.notFound().build();
    }

    String key = requestURI.substring(index + prefix.length());
    // Decode in case of spaces etc.
    key = java.net.URLDecoder.decode(key, java.nio.charset.StandardCharsets.UTF_8);

    try {
      byte[] data = storageService.getOriginalFile(key);

      // Try to guess content type
      String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
      if (key.toLowerCase().endsWith(".jpg") || key.toLowerCase().endsWith(".jpeg"))
        contentType = "image/jpeg";
      else if (key.toLowerCase().endsWith(".png"))
        contentType = "image/png";
      else if (key.toLowerCase().endsWith(".gif"))
        contentType = "image/gif";
      else if (key.toLowerCase().endsWith(".webp"))
        contentType = "image/webp";

      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_TYPE, contentType)
          .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
          .body(data);
    } catch (Exception e) {
      return ResponseEntity.notFound().build();
    }
  }
}

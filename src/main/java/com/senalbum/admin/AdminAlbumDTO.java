package com.senalbum.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminAlbumDTO {
  private UUID id;
  private String title;
  private String ownerName;
  private String ownerEmail;
  private String coverImageUrl;
  private int photoCount;
  private long viewCount;
  private long downloadCount;
  private long sizeBytes;
  private String status; // active, expired, protected
  private LocalDateTime createdAt;
  private LocalDateTime expiresAt;
}

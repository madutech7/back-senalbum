package com.senalbum.album.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AlbumUpdateRequest {
  private String title;
  private String description;
  private LocalDateTime expiresAt;
  private String password;
}

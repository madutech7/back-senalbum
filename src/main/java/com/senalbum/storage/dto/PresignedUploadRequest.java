package com.senalbum.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUploadRequest {
  private String filename;
  private String contentType;
  private String albumId;
}

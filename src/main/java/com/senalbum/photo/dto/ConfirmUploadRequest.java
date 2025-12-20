package com.senalbum.photo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmUploadRequest {
  private String originalKey;
  private String previewKey;
  private String filename;
  private Long size;
  private String mimeType;
}

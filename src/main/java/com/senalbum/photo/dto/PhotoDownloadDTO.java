package com.senalbum.photo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PhotoDownloadDTO {
  private byte[] data;
  private String filename;
}

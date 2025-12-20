package com.senalbum.photographer.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
  private String firstName;
  private String lastName;
  private String brandName;
  private String brandPrimaryColor;
  private String customDomain;
  private String profilePictureUrl;
  // Logo is handled via separate upload or passed as URL if uploaded first
  private String brandLogoUrl;
  private String brandCoverUrl;
  private Boolean watermarkEnabled;
  private Boolean notifyDownloads;
  private Boolean notifyViews;
}

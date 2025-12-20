package com.senalbum.photographer;

public enum SubscriptionPlan {
  FREE(2, 500L * 1024 * 1024, 14, false), // 2 albums, 500 MB, 14 days
  PRO(20, 10L * 1024 * 1024 * 1024, -1, true), // 20 albums/month, 10 GB, Unlimited duration
  STUDIO(-1, 50L * 1024 * 1024 * 1024, -1, true); // Unlimited albums, 50 GB, Unlimited duration

  private final int maxAlbums; // -1 for unlimited
  private final long maxStorageBytes;
  private final int validityDays; // -1 for unlimited
  private final boolean customBranding;

  SubscriptionPlan(int maxAlbums, long maxStorageBytes, int validityDays, boolean customBranding) {
    this.maxAlbums = maxAlbums;
    this.maxStorageBytes = maxStorageBytes;
    this.validityDays = validityDays;
    this.customBranding = customBranding;
  }

  public int getMaxAlbums() {
    return maxAlbums;
  }

  public long getMaxStorageBytes() {
    return maxStorageBytes;
  }

  public int getValidityDays() {
    return validityDays;
  }

  public boolean isCustomBranding() {
    return customBranding;
  }
}

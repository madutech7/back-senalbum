package com.senalbum.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsDTO {
  private long totalUsers;
  private long proUsers;
  private long enterpriseUsers;
  private long totalAlbums;
  private long totalPhotos;
  private long totalViews;
  private long totalDownloads;
  private long totalStorageBytes;
  private double monthlyRevenue; // in local currency (CFA)
  private long newUsersThisMonth;
  private long newAlbumsThisMonth;
}

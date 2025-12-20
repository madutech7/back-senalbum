package com.senalbum.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardStatsResponse {
  private long activeAlbums;
  private long totalViews;
  private long totalDownloads;
  private long usedStorage; // Octets
  private long totalStorage; // Octets
  private long maxAlbums;
  private int extraCredits;
}

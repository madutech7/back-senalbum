package com.senalbum.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDTO {
  private UUID id;
  private String email;
  private String firstName;
  private String lastName;
  private String profilePictureUrl;
  private String subscriptionPlan;
  private LocalDateTime createdAt;
  private long albumCount;
  private long storageUsed;
  private String status; // active, inactive, suspended
  private LocalDateTime lastActiveAt;
}

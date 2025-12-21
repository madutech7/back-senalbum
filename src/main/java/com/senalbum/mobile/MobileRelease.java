package com.senalbum.mobile;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "mobile_releases")
@Data
public class MobileRelease {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false)
  private String version;

  @Column(nullable = false)
  private String wasabiKey;

  @Column(length = 2000)
  private String changelog;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  private int downloadCount = 0;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}

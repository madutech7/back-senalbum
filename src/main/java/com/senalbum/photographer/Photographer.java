package com.senalbum.photographer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant un photographe
 */
@Entity
@Table(name = "photographers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Photographer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(nullable = false)
    private String password; // Hashé avec BCrypt

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan")
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.FREE;

    // Branding / White Label fields
    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "brand_logo_url")
    private String brandLogoUrl;

    @Column(name = "brand_cover_url")
    private String brandCoverUrl;

    @Column(name = "brand_primary_color")
    private String brandPrimaryColor;

    @Column(name = "custom_domain", unique = true)
    private String customDomain; // e.g. "mybrand" -> mybrand.senalbum.com or full domain

    @Column(name = "watermark_enabled")
    private Boolean watermarkEnabled = false;

    @Column(name = "extra_album_credits")
    private Integer extraAlbumCredits = 0;

    @Column(name = "notify_downloads")
    private Boolean notifyDownloads = true;

    @Column(name = "notify_views")
    private Boolean notifyViews = false;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_code_expires_at")
    private LocalDateTime verificationCodeExpiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

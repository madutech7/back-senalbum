package com.senalbum.album.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class AlbumResponse {
    private UUID id;
    private String title;
    private String description;
    private String token;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private String coverImageUrl;

    @JsonProperty("isPasswordProtected")
    private boolean isPasswordProtected;

    // Branding info
    private String brandName;
    private String brandLogoUrl;
    private String brandCoverUrl;
    private String brandPrimaryColor;

    private Long viewCount;
    private Long downloadCount;

}

package com.senalbum.album.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlbumCreateRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private LocalDateTime expiresAt;

    private String password;
}

package com.senalbum.photo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class PhotoResponse {
    private UUID id;
    private String previewUrl;
    private String downloadUrl;
    private LocalDateTime createdAt;
}


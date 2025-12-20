package com.senalbum.photo;

import com.senalbum.album.Album;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant une photo
 */
@Entity
@Table(name = "photos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", nullable = false)
    private Album album;

    @Column(name = "original_path", nullable = false)
    private String originalPath; // Chemin vers la version HD originale

    @Column(name = "original_filename")
    private String originalFilename; // Nom original du fichier

    @Column(name = "preview_path", nullable = false)
    private String previewPath; // Chemin vers la version preview compressée

    @Column(name = "size")
    private Long size = 0L; // Taille en octets

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

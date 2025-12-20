package com.senalbum.album;

import com.senalbum.photographer.Photographer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlbumRepository extends JpaRepository<Album, UUID> {
    @Query("SELECT SUM(a.viewCount) FROM Album a WHERE a.photographer.id = :photographerId")
    Long getTotalViews(@Param("photographerId") UUID photographerId);

    @Query("SELECT SUM(a.downloadCount) FROM Album a WHERE a.photographer.id = :photographerId")
    Long getTotalDownloads(@Param("photographerId") UUID photographerId);

    List<Album> findByPhotographerOrderByCreatedAtDesc(Photographer photographer);

    Optional<Album> findByToken(String token);

    @Query("SELECT a FROM Album a WHERE REPLACE(a.photographer.brandName, ' ', '-') = :brandName AND REPLACE(a.title, ' ', '-') = :title")
    Optional<Album> findByBrandNameAndTitle(@Param("brandName") String brandName, @Param("title") String title);

    boolean existsByToken(String token);

    long countByPhotographer(Photographer photographer);

    long countByPhotographerAndCreatedAtAfter(Photographer photographer, java.time.LocalDateTime date);

    // Admin global statistics
    @Query("SELECT SUM(a.viewCount) FROM Album a")
    Long getGlobalTotalViews();

    @Query("SELECT SUM(a.downloadCount) FROM Album a")
    Long getGlobalTotalDownloads();

    long countByCreatedAtAfter(java.time.LocalDateTime date);
}

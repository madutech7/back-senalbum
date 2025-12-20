package com.senalbum.photo;

import com.senalbum.album.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, UUID> {
    List<Photo> findByAlbumOrderByCreatedAtAsc(Album album);

    void deleteByAlbum(Album album);

    @Query("SELECT COALESCE(SUM(p.size), 0) FROM Photo p WHERE p.album.id IN (SELECT a.id FROM Album a WHERE a.photographer.id = :photographerId)")
    Long getTotalSize(@Param("photographerId") UUID photographerId);

    // Admin global statistics
    @Query("SELECT COALESCE(SUM(p.size), 0) FROM Photo p")
    Long getGlobalTotalSize();
}

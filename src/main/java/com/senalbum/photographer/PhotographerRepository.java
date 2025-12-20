package com.senalbum.photographer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhotographerRepository extends JpaRepository<Photographer, UUID> {
    Optional<Photographer> findByEmail(String email);

    Optional<Photographer> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    // Admin statistics
    long countByCreatedAtAfter(java.time.LocalDateTime date);
}

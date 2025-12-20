package com.senalbum.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FileContentRepository extends JpaRepository<FileContent, UUID> {
}

package com.senalbum.mobile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface MobileReleaseRepository extends JpaRepository<MobileRelease, String> {
  Optional<MobileRelease> findFirstByOrderByCreatedAtDesc();

  List<MobileRelease> findAllByOrderByCreatedAtDesc();
}

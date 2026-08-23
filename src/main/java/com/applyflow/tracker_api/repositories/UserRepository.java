package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.User;
import org.springframework.cache.annotation.Cacheable; // 👈 Add this import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Cache user objects by their ID. Subsequent calls return instantly without
    // SQL.
    @Cacheable(value = "users", key = "#id")
    Optional<User> findById(Long id);

    Optional<User> findByGoogleSub(String googleSub);

    Optional<User> findByEmail(String email);

    List<User> findByDeletionRequestedAtBefore(LocalDateTime cutoff);
}

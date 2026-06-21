package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // Fetch a paged list of applications belonging strictly to the logged-in user
    Page<Application> findByUserId(Long userId, Pageable pageable);

    // Find a specific application only if it belongs to the logged-in user
    Optional<Application> findByIdAndUserId(Long id, Long userId);
}
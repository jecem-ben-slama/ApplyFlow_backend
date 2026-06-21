package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.CvVariant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CvVariantRepository extends JpaRepository<CvVariant, Long> {

    // Fetch CVs matching a language, isolated strictly to the current user
    Page<CvVariant> findByUserId(Long userId, Pageable pageable);

    Page<CvVariant> findByUserIdAndLanguage(Long userId, String language, Pageable pageable);

    // Find a specific CV only if it belongs to the logged-in user
    Optional<CvVariant> findByIdAndUserId(Long id, Long userId);
}
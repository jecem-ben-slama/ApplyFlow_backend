package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.CvVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CvVariantRepository extends JpaRepository<CvVariant, Long> {

    Optional<CvVariant> findByIdAndUserId(Long id, Long userId);

    @Query("""
            SELECT c FROM CvVariant c
            WHERE c.user.id = :userId
              AND (:language IS NULL OR c.language = :language)
              AND (:search IS NULL OR
                   LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<CvVariant> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("language") String language,
            @Param("search") String search,
            Pageable pageable);
}
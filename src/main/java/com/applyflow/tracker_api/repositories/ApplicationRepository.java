package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Page<Application> findByUserId(Long userId, Pageable pageable);

    Optional<Application> findByIdAndUserId(Long id, Long userId);

    // Filter by status only
    Page<Application> findByUserIdAndStatusIgnoreCase(Long userId, String status, Pageable pageable);

    // Search by keyword across companyName and jobTitle only
    @Query("SELECT a FROM Application a WHERE a.user.id = :userId AND " +
            "(LOWER(a.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.jobTitle) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Application> searchByKeyword(@Param("userId") Long userId,
            @Param("keyword") String keyword,
            Pageable pageable);

    // Search by keyword AND filter by status combined
    @Query("SELECT a FROM Application a WHERE a.user.id = :userId AND " +
            "LOWER(a.status) = LOWER(:status) AND " +
            "(LOWER(a.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.jobTitle) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Application> searchByKeywordAndStatus(@Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable);
}
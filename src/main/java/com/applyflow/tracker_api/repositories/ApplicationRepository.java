package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.dtos.ApplicationSummaryDto;
import com.applyflow.tracker_api.models.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

        // Eagerly join skills collection on base lookup
        @EntityGraph(attributePaths = { "skills" })
        Page<Application> findByUserId(Long userId, Pageable pageable);

        Optional<Application> findByIdAndUserId(Long id, Long userId);

        // Eagerly join skills collection when filtering by status only
        @EntityGraph(attributePaths = { "skills" })
        Page<Application> findByUserIdAndStatusIgnoreCase(Long userId, String status, Pageable pageable);

        // Eagerly join skills collection when searching by keyword
        @EntityGraph(attributePaths = { "skills" })
        @Query("SELECT a FROM Application a WHERE a.user.id = :userId AND " +
                        "(LOWER(a.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(a.jobTitle) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<Application> searchByKeyword(@Param("userId") Long userId,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        // Eagerly join skills collection when filtering by keyword AND status combined
        @EntityGraph(attributePaths = { "skills" })
        @Query("SELECT a FROM Application a WHERE a.user.id = :userId AND " +
                        "LOWER(a.status) = LOWER(:status) AND " +
                        "(LOWER(a.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(a.jobTitle) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<Application> searchByKeywordAndStatus(@Param("userId") Long userId,
                        @Param("keyword") String keyword,
                        @Param("status") String status,
                        Pageable pageable);

        // Total application count for a user, for the stats summary (all-time).
        long countByUserId(Long userId);

        // Date-range aware total, keyed off dateApplied. from/to are always
        // non-null effective bounds (resolved by the service via DateRangeUtils).
        @Query("""
                        SELECT COUNT(a) FROM Application a
                        WHERE a.user.id = :userId
                        AND a.dateApplied >= :from
                        AND a.dateApplied <= :to
                        """)
        long countByUserIdInRange(@Param("userId") Long userId,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);

        // Current-status count, for the active/stalled split.
        long countByUserIdAndStatusIn(Long userId, List<String> statuses);

        // Lightweight list for the Kanban board and the timeline dropdown — no
        // pagination, no skills join, just enough to render a card.
        @Query("""
                        SELECT new com.applyflow.tracker_api.dtos.ApplicationSummaryDto(
                                a.id, a.companyName, a.jobTitle, a.status)
                        FROM Application a
                        WHERE a.user.id = :userId
                        ORDER BY a.id DESC
                        """)
        List<ApplicationSummaryDto> findSummariesByUserId(@Param("userId") Long userId);
}
package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.ApplicationEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ApplicationEventRepository extends JpaRepository<ApplicationEvent, Long> {

        // Full history for one application's timeline view.
        List<ApplicationEvent> findByApplicationIdOrderByOccurredAtAsc(Long applicationId);

        // All events of a given status for a user, earliest first — used to find each
        // application's *first* time reaching that status (e.g. first SENT, first
        // RESPONDED).
        List<ApplicationEvent> findByApplication_User_IdAndStatusOrderByOccurredAtAsc(
                        Long userId, String status);

        // Most recent activity across all of a user's applications, for an activity
        // feed.
        @Query("""
                        SELECT e FROM ApplicationEvent e
                        WHERE e.application.user.id = :userId
                        ORDER BY e.occurredAt DESC
                        """)
        List<ApplicationEvent> findRecentByUser(@Param("userId") Long userId, Pageable pageable);

        // Distinct application count per status, for the funnel (unfiltered case).
        // from/to are always non-null effective bounds (resolved by the service via
        // DateRangeUtils) — Postgres can't infer parameter types from an
        // "IS NULL OR ..." pattern, so we never send a literal null here.
        @Query("""
                        SELECT e.status, COUNT(DISTINCT e.application.id)
                        FROM ApplicationEvent e
                        WHERE e.application.user.id = :userId
                        AND e.occurredAt >= :from
                        AND e.occurredAt <= :to
                        GROUP BY e.status
                        """)
        List<Object[]> countDistinctApplicationsByStatusForUser(
                        @Param("userId") Long userId,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);

        // Filtered version of the funnel query, used when the caller supplied
        // jobTitle/template/cvVariant/status filters. appIds is resolved by the
        // service via ApplicationRepository.findApplicationIdsByUserIdAndFilters
        // and is guaranteed non-empty before this is called.
        @Query("""
                        SELECT e.status, COUNT(DISTINCT e.application.id)
                        FROM ApplicationEvent e
                        WHERE e.application.user.id = :userId
                        AND e.application.id IN :appIds
                        AND e.occurredAt >= :from
                        AND e.occurredAt <= :to
                        GROUP BY e.status
                        """)
        List<Object[]> countDistinctApplicationsByStatusForUserAndApplicationIds(
                        @Param("userId") Long userId,
                        @Param("appIds") List<Long> applicationIds,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);

        // Date-range aware version of the "first reached status X" lookups used for
        // response-time, never-viewed, and rejection-stage calculations. from/to are
        // always non-null effective bounds, same reasoning as above.
        @Query("""
                        SELECT e FROM ApplicationEvent e
                        WHERE e.application.user.id = :userId
                        AND e.status = :status
                        AND e.occurredAt >= :from
                        AND e.occurredAt <= :to
                        ORDER BY e.occurredAt ASC
                        """)
        List<ApplicationEvent> findByApplication_User_IdAndStatusInRange(
                        @Param("userId") Long userId,
                        @Param("status") String status,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);

        // appIds is always non-null and non-empty here — callers in StatsService
        // guard against null/empty applicationIds before invoking this method.
        // (Previously used "(:appIds IS NULL OR ...)" which made Postgres unable to
        // infer the parameter type for the IN-list, causing
        // "could not determine data type of parameter" errors.)
        @Query("""
                        SELECT COUNT(DISTINCT e.application.id)
                        FROM ApplicationEvent e
                        WHERE e.application.user.id = :userId
                        AND e.status = :status
                        AND e.application.id IN :appIds
                        AND (CAST(:from AS timestamp) IS NULL OR e.occurredAt >= :from)
                        AND (CAST(:to AS timestamp) IS NULL OR e.occurredAt <= :to)
                        """)
        long countDistinctApplicationsByStatusAndApplicationIds(
                        @Param("userId") Long userId,
                        @Param("status") String status,
                        @Param("appIds") List<Long> applicationIds,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);

        // Same appIds guarantee as above — callers always pass a non-null,
        // non-empty list.
        @Query("""
                        SELECT e FROM ApplicationEvent e
                        WHERE e.application.user.id = :userId
                        AND e.status = :status
                        AND e.application.id IN :appIds
                        AND (CAST(:from AS timestamp) IS NULL OR e.occurredAt >= :from)
                        AND (CAST(:to AS timestamp) IS NULL OR e.occurredAt <= :to)
                        ORDER BY e.occurredAt ASC
                        """)
        List<ApplicationEvent> findEarliestEventByApplicationIds(
                        @Param("userId") Long userId,
                        @Param("status") String status,
                        @Param("appIds") List<Long> applicationIds,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);

        // Batch fetch of full event history for a set of applications, grouped
        // in-memory by application id by the caller. Replaces the per-application
        // N+1 lookup that used to run inside getRejectionStageBreakdown's loop.
        @Query("""
                        SELECT e FROM ApplicationEvent e
                        WHERE e.application.id IN :appIds
                        ORDER BY e.application.id ASC, e.occurredAt ASC
                        """)
        List<ApplicationEvent> findByApplicationIdInOrderByApplicationIdAscOccurredAtAsc(
                        @Param("appIds") List<Long> applicationIds);

        // Batch fetch for trend charts: every SENT/RESPONDED/VIEWED/INTERVIEWING/
        // OFFER event for a set of applications within [from, to], fetched once
        // for the whole requested range. StatsService.getTrendData groups these
        // by occurredAt.toLocalDate() + status in memory instead of calling
        // countApplicationsThatReached once per status per day.
        @Query("""
                        SELECT e FROM ApplicationEvent e
                        WHERE e.application.id IN :appIds
                        AND e.status IN :statuses
                        AND e.occurredAt >= :from
                        AND e.occurredAt <= :to
                        ORDER BY e.occurredAt ASC
                        """)
        List<ApplicationEvent> findByApplicationIdInAndStatusInAndOccurredAtBetween(
                        @Param("appIds") List<Long> applicationIds,
                        @Param("statuses") List<String> statuses,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);
}
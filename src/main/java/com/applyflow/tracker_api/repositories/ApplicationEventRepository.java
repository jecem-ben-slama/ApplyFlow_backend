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

        // Distinct application count per status, for the funnel. from/to are always
        // non-null effective bounds (resolved by the service via DateRangeUtils) —
        // Postgres can't infer parameter types from an "IS NULL OR ..." pattern, so
        // we never send a literal null here.
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
}
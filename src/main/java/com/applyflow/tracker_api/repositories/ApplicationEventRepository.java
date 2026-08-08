package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.ApplicationEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Distinct application count per status, for the funnel: "how many applications
    // ever reached this stage" rather than "how many are currently sitting in it."
    @Query("""
            SELECT e.status, COUNT(DISTINCT e.application.id)
            FROM ApplicationEvent e
            WHERE e.application.user.id = :userId
            GROUP BY e.status
            """)
    List<Object[]> countDistinctApplicationsByStatusForUser(@Param("userId") Long userId);
}
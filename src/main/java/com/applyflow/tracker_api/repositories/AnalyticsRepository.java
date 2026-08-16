package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.dtos.StatMetricDto;
import com.applyflow.tracker_api.models.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<Application, Long> {

        // successStatuses is always resolved by the service (defaults applied there
        // if the caller passed none). from/to are always non-null effective bounds
        // (resolved via DateRangeUtils) — Postgres can't infer parameter types from
        // an "IS NULL OR ..." pattern, so we never send a literal null here.
        //
        // "Success" is determined by event history (did this application EVER reach
        // one of successStatuses, e.g. INTERVIEWING/OFFER), not by the application's
        // CURRENT status. Filtering on a.status IN :successStatuses undercounts:
        // an application that reached INTERVIEWING and was later REJECTED has a
        // current status of REJECTED, so it would silently drop out of the success
        // count even though it demonstrably succeeded at that stage. The correlated
        // EXISTS subquery below checks the application's full ApplicationEvent
        // history instead, matching the "ever reached X" semantics StatsService
        // already uses elsewhere (see countApplicationsThatReached).

        @Query("SELECT new com.applyflow.tracker_api.dtos.StatMetricDto(" +
                        "COALESCE(cv.name, 'No CV'), COUNT(a), " +
                        "SUM(CASE WHEN EXISTS (" +
                        "    SELECT 1 FROM ApplicationEvent e " +
                        "    WHERE e.application = a AND e.status IN :successStatuses" +
                        ") THEN 1 ELSE 0 END), 0.0) " +
                        "FROM Application a LEFT JOIN a.cvVariant cv " +
                        "WHERE a.user.id = :userId " +
                        "AND a.dateApplied >= :from AND a.dateApplied <= :to " +
                        "GROUP BY cv.name")
        List<StatMetricDto> getStatsByCvVariant(@Param("userId") Long userId,
                        @Param("successStatuses") List<String> successStatuses,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);

        @Query("SELECT new com.applyflow.tracker_api.dtos.StatMetricDto(" +
                        "a.language, COUNT(a), " +
                        "SUM(CASE WHEN EXISTS (" +
                        "    SELECT 1 FROM ApplicationEvent e " +
                        "    WHERE e.application = a AND e.status IN :successStatuses" +
                        ") THEN 1 ELSE 0 END), 0.0) " +
                        "FROM Application a " +
                        "WHERE a.user.id = :userId " +
                        "AND a.dateApplied >= :from AND a.dateApplied <= :to " +
                        "GROUP BY a.language")
        List<StatMetricDto> getStatsByLanguage(@Param("userId") Long userId,
                        @Param("successStatuses") List<String> successStatuses,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);

        @Query("SELECT new com.applyflow.tracker_api.dtos.StatMetricDto(" +
                        "a.jobTitle, COUNT(a), " +
                        "SUM(CASE WHEN EXISTS (" +
                        "    SELECT 1 FROM ApplicationEvent e " +
                        "    WHERE e.application = a AND e.status IN :successStatuses" +
                        ") THEN 1 ELSE 0 END), 0.0) " +
                        "FROM Application a " +
                        "WHERE a.user.id = :userId " +
                        "AND a.dateApplied >= :from AND a.dateApplied <= :to " +
                        "GROUP BY a.jobTitle")
        List<StatMetricDto> getStatsByJobTitle(@Param("userId") Long userId,
                        @Param("successStatuses") List<String> successStatuses,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);

        // Template grouping, same shape as the three above.
        // Assumption: Template has a getName() field — adjust t.name if it differs.
        @Query("SELECT new com.applyflow.tracker_api.dtos.StatMetricDto(" +
                        "COALESCE(t.name, 'No template'), COUNT(a), " +
                        "SUM(CASE WHEN EXISTS (" +
                        "    SELECT 1 FROM ApplicationEvent e " +
                        "    WHERE e.application = a AND e.status IN :successStatuses" +
                        ") THEN 1 ELSE 0 END), 0.0) " +
                        "FROM Application a LEFT JOIN a.template t " +
                        "WHERE a.user.id = :userId " +
                        "AND a.dateApplied >= :from AND a.dateApplied <= :to " +
                        "GROUP BY t.name")
        List<StatMetricDto> getStatsByTemplate(@Param("userId") Long userId,
                        @Param("successStatuses") List<String> successStatuses,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);
}
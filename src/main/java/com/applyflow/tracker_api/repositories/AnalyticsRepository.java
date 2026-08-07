package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.dtos.StatMetricDto;
import com.applyflow.tracker_api.models.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<Application, Long> {

    // Stats grouped by CV Variant name
    @Query("SELECT new com.applyflow.tracker_api.dtos.StatMetricDto(" +
            "COALESCE(cv.name, 'No CV'), COUNT(a), " +
            "SUM(CASE WHEN a.status IN ('INTERVIEW', 'OFFER') THEN 1 ELSE 0 END), 0.0) " +
            "FROM Application a LEFT JOIN a.cvVariant cv WHERE a.user.id = :userId GROUP BY cv.name")
    List<StatMetricDto> getStatsByCvVariant(@Param("userId") Long userId);

    // Stats grouped by Application Language
    @Query("SELECT new com.applyflow.tracker_api.dtos.StatMetricDto(" +
            "a.language, COUNT(a), " +
            "SUM(CASE WHEN a.status IN ('INTERVIEW', 'OFFER') THEN 1 ELSE 0 END), 0.0) " +
            "FROM Application a WHERE a.user.id = :userId GROUP BY a.language")
    List<StatMetricDto> getStatsByLanguage(@Param("userId") Long userId);

    // Stats grouped by Job Title / Post
    @Query("SELECT new com.applyflow.tracker_api.dtos.StatMetricDto(" +
            "a.jobTitle, COUNT(a), " +
            "SUM(CASE WHEN a.status IN ('INTERVIEW', 'OFFER') THEN 1 ELSE 0 END), 0.0) " +
            "FROM Application a WHERE a.user.id = :userId GROUP BY a.jobTitle")
    List<StatMetricDto> getStatsByJobTitle(@Param("userId") Long userId);
}
package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // Retrieves only the applications belonging to a specific user, ordered by
    // newest first
    List<Application> findByUserIdOrderByDateAppliedDesc(Long userId);
}
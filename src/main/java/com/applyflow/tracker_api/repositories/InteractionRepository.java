package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.ApplicationInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InteractionRepository extends JpaRepository<ApplicationInteraction, Long> {

    // Allows you to find all interactions for a specific application
    // (Great for showing a timeline in your CRM later!)
    List<ApplicationInteraction> findByApplicationIdOrderByTimestampDesc(Long applicationId);
}
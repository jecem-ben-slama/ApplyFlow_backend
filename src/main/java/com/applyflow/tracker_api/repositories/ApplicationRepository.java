package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    // Inherits all built-in CRUD and pagination methods
}
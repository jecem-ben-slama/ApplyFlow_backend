package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    // Optional utility method to filter templates by language cleanly on your UI
    // later
    Page<Template> findByLanguage(String language, Pageable pageable);
}
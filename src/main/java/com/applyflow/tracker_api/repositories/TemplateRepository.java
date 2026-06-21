package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {

    // Filter templates by language, isolated strictly to the current user
    Page<Template> findByUserIdAndLanguage(Long userId, String language, Pageable pageable);

    // Fetch a paged list of templates belonging strictly to the logged-in user
    Page<Template> findByUserId(Long userId, Pageable pageable);

    // Find a specific template only if it belongs to the logged-in user
    Optional<Template> findByIdAndUserId(Long id, Long userId);
}
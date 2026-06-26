package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {

    Page<Template> findByUserIdAndLanguage(Long userId, String language, Pageable pageable);

    Page<Template> findByUserId(Long userId, Pageable pageable);

    Optional<Template> findByIdAndUserId(Long id, Long userId);

    @Query("""
                SELECT t FROM Template t
                WHERE t.user.id = :userId
                AND (
                    LOWER(t.name)            LIKE LOWER(CONCAT('%', :term, '%')) OR
                    LOWER(t.subjectTemplate) LIKE LOWER(CONCAT('%', :term, '%')) OR
                    LOWER(t.bodyTemplate)    LIKE LOWER(CONCAT('%', :term, '%'))
                )
            """)
    Page<Template> searchByUserIdAndTerm(
            @Param("userId") Long userId,
            @Param("term") String term,
            Pageable pageable);

    @Query("""
                SELECT t FROM Template t
                WHERE t.user.id = :userId
                AND t.language = :language
                AND (
                    LOWER(t.name)            LIKE LOWER(CONCAT('%', :term, '%')) OR
                    LOWER(t.subjectTemplate) LIKE LOWER(CONCAT('%', :term, '%')) OR
                    LOWER(t.bodyTemplate)    LIKE LOWER(CONCAT('%', :term, '%'))
                )
            """)
    Page<Template> searchByUserIdAndLanguageAndTerm(
            @Param("userId") Long userId,
            @Param("language") String language,
            @Param("term") String term,
            Pageable pageable);
}
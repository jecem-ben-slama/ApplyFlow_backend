package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.Skill;
import com.applyflow.tracker_api.models.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByUserIdAndName(Long userId, String name);

    Page<Skill> findByUserId(Long userId, Pageable pageable);

    Page<Skill> findByUserIdAndCategoryId(Long userId, Long categoryId, Pageable pageable);

    Optional<Skill> findByIdAndUserId(Long id, Long userId);

    Optional<Skill> findByName(String name);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Skill s SET s.user = :realUser WHERE s.user.id = :guestId")
    void reassignOwner(@Param("guestId") Long guestId, @Param("realUser") User realUser);

    @Query("""
                SELECT s FROM Skill s
                WHERE s.user.id = :userId
                AND (
                    LOWER(s.name)       LIKE LOWER(CONCAT('%', :term, '%')) OR
                    LOWER(s.sentenceEn) LIKE LOWER(CONCAT('%', :term, '%')) OR
                    LOWER(s.sentenceFr) LIKE LOWER(CONCAT('%', :term, '%'))
                )
            """)
    Page<Skill> searchByUserIdAndTerm(
            @Param("userId") Long userId,
            @Param("term") String term,
            Pageable pageable);

    @Query("""
                SELECT s FROM Skill s
                WHERE s.user.id = :userId
                AND s.category.id = :categoryId
                AND (
                    LOWER(s.name)       LIKE LOWER(CONCAT('%', :term, '%')) OR
                    LOWER(s.sentenceEn) LIKE LOWER(CONCAT('%', :term, '%')) OR
                    LOWER(s.sentenceFr) LIKE LOWER(CONCAT('%', :term, '%'))
                )
            """)
    Page<Skill> searchByUserIdAndCategoryIdAndTerm(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("term") String term,
            Pageable pageable);
}
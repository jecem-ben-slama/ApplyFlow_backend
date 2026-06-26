package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.Skill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByUserIdAndName(Long userId, String name);

    Page<Skill> findByUserId(Long userId, Pageable pageable);

    Page<Skill> findByUserIdAndCategoryId(Long userId, Long categoryId, Pageable pageable);

    Optional<Skill> findByIdAndUserId(Long id, Long userId);

    Optional<Skill> findByName(String name);
}
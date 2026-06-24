package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.Skill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    // Prevent duplicate skills for a specific user during data creation workflows
    Optional<Skill> findByUserIdAndName(Long userId, String Name);

    // Fetch a paged list of skills belonging strictly to the logged-in user
    Page<Skill> findByUserId(Long userId, Pageable pageable);

    // Find a specific skill only if it belongs to the logged-in user
    Optional<Skill> findByIdAndUserId(Long id, Long userId);
    
    Optional<Skill> findByName(String Name);

}
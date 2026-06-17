package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.models.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    // Used to look up skills in lowercase/trimmed format to prevent duplicates
    Optional<Skill> findByTechnicalName(String technicalName);
}
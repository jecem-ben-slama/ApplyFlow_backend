package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.models.Skill;
import com.applyflow.tracker_api.repositories.SkillRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;

    // CREATE
    @Transactional
    public Skill createSkill(Skill skill) {
        // Standardize the technical name to lowercase and trim spaces
        String cleanTechName = skill.getTechnicalName().toLowerCase().trim();

        if (skillRepository.findByTechnicalName(cleanTechName).isPresent()) {
            throw new IllegalArgumentException("A skill with technical name '" + cleanTechName + "' already exists.");
        }

        skill.setTechnicalName(cleanTechName);
        return skillRepository.save(skill);
    }

    public Page<Skill> getAllSkills(Pageable pageable) {
        return skillRepository.findAll(pageable);
    }

    // READ ONE
    public Skill getSkillById(Long id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Skill not found with ID: " + id));
    }

    // UPDATE
    @Transactional
    public Skill updateSkill(Long id, Skill skillDetails) {
        Skill existingSkill = getSkillById(id);

        existingSkill.setDisplayName(skillDetails.getDisplayName());
        existingSkill.setSentenceEn(skillDetails.getSentenceEn());
        existingSkill.setSentenceFr(skillDetails.getSentenceFr());

        // Update technical name carefully while preserving unique constraint checks
        String cleanTechName = skillDetails.getTechnicalName().toLowerCase().trim();
        if (!existingSkill.getTechnicalName().equals(cleanTechName) &&
                skillRepository.findByTechnicalName(cleanTechName).isPresent()) {
            throw new IllegalArgumentException("Another skill already uses the name: " + cleanTechName);
        }
        existingSkill.setTechnicalName(cleanTechName);

        return skillRepository.save(existingSkill);
    }

    // DELETE
    @Transactional
    public void deleteSkill(Long id) {
        Skill skill = getSkillById(id);
        skillRepository.delete(skill);
    }
}
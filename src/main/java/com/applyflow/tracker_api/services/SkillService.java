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

    @Transactional
    public Skill createSkill(Skill skill) {
        String cleanTechName = skill.getTechnicalName().toLowerCase().trim();

        // Check uniqueness strictly per user
        if (skillRepository.findByUserIdAndTechnicalName(skill.getUser().getId(), cleanTechName).isPresent()) {
            throw new IllegalArgumentException(
                    "A skill with technical name '" + cleanTechName + "' already exists for this user.");
        }

        skill.setTechnicalName(cleanTechName);
        return skillRepository.save(skill);
    }

    public Page<Skill> getSkillsForUser(Long userId, Pageable pageable) {
        return skillRepository.findByUserId(userId, pageable);
    }

    public Skill getSkillByIdAndUser(Long id, Long userId) {
        return skillRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Skill not found or access denied."));
    }

    @Transactional
    public Skill updateSkill(Long id, Long userId, Skill skillDetails) {
        Skill existingSkill = getSkillByIdAndUser(id, userId);

        existingSkill.setDisplayName(skillDetails.getDisplayName());
        existingSkill.setSentenceEn(skillDetails.getSentenceEn());
        existingSkill.setSentenceFr(skillDetails.getSentenceFr());

        String cleanTechName = skillDetails.getTechnicalName().toLowerCase().trim();
        if (!existingSkill.getTechnicalName().equals(cleanTechName) &&
                skillRepository.findByUserIdAndTechnicalName(userId, cleanTechName).isPresent()) {
            throw new IllegalArgumentException("Another skill already uses the name: " + cleanTechName);
        }
        existingSkill.setTechnicalName(cleanTechName);

        return skillRepository.save(existingSkill);
    }

    @Transactional
    public void deleteSkill(Long id, Long userId) {
        Skill skill = getSkillByIdAndUser(id, userId);
        skillRepository.delete(skill);
    }
}
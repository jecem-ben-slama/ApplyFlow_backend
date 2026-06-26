package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.models.Category;
import com.applyflow.tracker_api.models.Skill;
import com.applyflow.tracker_api.repositories.CategoryRepository;
import com.applyflow.tracker_api.repositories.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public Skill createSkill(Skill skill, Long categoryId, Long userId) {
        String cleanName = skill.getName().toLowerCase().trim();

        if (skillRepository.findByUserIdAndName(userId, cleanName).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A skill with name '" + cleanName + "' already exists.");
        }

        if (categoryId != null) {
            Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Category not found."));
            skill.setCategory(category);
        }

        skill.setName(cleanName);
        return skillRepository.save(skill);
    }

    public Page<Skill> getSkillsForUser(Long userId, Long categoryId, Pageable pageable) {
        if (categoryId != null) {
            return skillRepository.findByUserIdAndCategoryId(userId, categoryId, pageable);
        }
        return skillRepository.findByUserId(userId, pageable);
    }

    public Skill getSkillByIdAndUser(Long id, Long userId) {
        return skillRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Skill not found or access denied."));
    }

    @Transactional
    public Skill updateSkill(Long id, Long userId, Skill skillDetails, Long categoryId) {
        Skill existing = getSkillByIdAndUser(id, userId);

        String cleanName = skillDetails.getName().toLowerCase().trim();
        if (!existing.getName().equals(cleanName) &&
                skillRepository.findByUserIdAndName(userId, cleanName).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Another skill already uses the name '" + cleanName + "'.");
        }

        existing.setName(cleanName);
        existing.setSentenceEn(skillDetails.getSentenceEn());
        existing.setSentenceFr(skillDetails.getSentenceFr());

        if (categoryId != null) {
            Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Category not found."));
            existing.setCategory(category);
        } else {
            // Passing null explicitly clears the category assignment
            existing.setCategory(null);
        }

        return skillRepository.save(existing);
    }

    @Transactional
    public void deleteSkill(Long id, Long userId) {
        Skill skill = getSkillByIdAndUser(id, userId);
        skillRepository.delete(skill);
    }
}
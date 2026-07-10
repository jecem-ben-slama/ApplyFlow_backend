package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.dtos.SkillDto;
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
    public SkillDto createSkill(Skill skill, Long categoryId, Long userId) {
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
        Skill saved = skillRepository.save(skill);
        return convertToDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<SkillDto> getSkillsForUser(Long userId, Long categoryId, String search, Pageable pageable) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasCategory = categoryId != null;
        Page<Skill> skillsPage;

        if (hasSearch && hasCategory) {
            skillsPage = skillRepository.searchByUserIdAndCategoryIdAndTerm(userId, categoryId, search, pageable);
        } else if (hasSearch) {
            skillsPage = skillRepository.searchByUserIdAndTerm(userId, search, pageable);
        } else if (hasCategory) {
            skillsPage = skillRepository.findByUserIdAndCategoryId(userId, categoryId, pageable);
        } else {
            skillsPage = skillRepository.findByUserId(userId, pageable);
        }

        return skillsPage.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public SkillDto getSkillByIdAndUser(Long id, Long userId) {
        Skill skill = skillRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Skill not found or access denied."));
        return convertToDto(skill);
    }

    @Transactional
    public SkillDto updateSkill(Long id, Long userId, Skill skillDetails, Long categoryId) {
        Skill existing = skillRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Skill not found or access denied."));

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
            existing.setCategory(null);
        }

        Skill updated = skillRepository.save(existing);
        return convertToDto(updated);
    }

    @Transactional
    public void deleteSkill(Long id, Long userId) {
        Skill skill = skillRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Skill not found or access denied."));
        skillRepository.delete(skill);
    }

    private SkillDto convertToDto(Skill skill) {
        return SkillDto.builder()
                .id(skill.getId())
                .name(skill.getName())
                .sentenceEn(skill.getSentenceEn())
                .sentenceFr(skill.getSentenceFr())
                .userId(skill.getUser() != null ? skill.getUser().getId() : null)
                .categoryId(skill.getCategory() != null ? skill.getCategory().getId() : null)
                .categoryName(skill.getCategory() != null ? skill.getCategory().getName() : null)
                .build();
    }
}
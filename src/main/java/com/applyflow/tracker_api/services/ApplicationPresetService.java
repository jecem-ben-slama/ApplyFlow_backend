package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.dtos.ApplicationPresetDto;
import com.applyflow.tracker_api.models.*;
import com.applyflow.tracker_api.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationPresetService {

    private final ApplicationPresetRepository presetRepository;
    private final TemplateRepository templateRepository;
    private final CvVariantRepository cvVariantRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;

    @Transactional
    public ApplicationPresetDto create(ApplicationPresetDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Template template = templateRepository.findByIdAndUserId(dto.getTemplateId(), userId)
                .orElseThrow(() -> new RuntimeException("Template not found or access denied"));

        CvVariant cvVariant = null;
        if (dto.getCvVariantId() != null) {
            cvVariant = cvVariantRepository.findByIdAndUserId(dto.getCvVariantId(), userId)
                    .orElseThrow(() -> new RuntimeException("CV Variant not found or access denied"));
        }

        Set<Skill> skills = resolveSkills(dto.getSkillIds(), userId);

        ApplicationPreset preset = ApplicationPreset.builder()
                .name(dto.getName())
                .jobTitle(dto.getJobTitle())
                .language(dto.getLanguage())
                .template(template)
                .cvVariant(cvVariant)
                .skills(skills)
                .notes(dto.getNotes())
                .user(user)
                .build();

        return toDto(presetRepository.save(preset));
    }

    @Transactional
    public ApplicationPresetDto update(Long id, ApplicationPresetDto dto, Long userId) {
        ApplicationPreset existing = presetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Preset not found or access denied"));

        if (dto.getName() != null)
            existing.setName(dto.getName());
        if (dto.getJobTitle() != null)
            existing.setJobTitle(dto.getJobTitle());
        if (dto.getLanguage() != null)
            existing.setLanguage(dto.getLanguage());
        if (dto.getNotes() != null)
            existing.setNotes(dto.getNotes());

        if (dto.getTemplateId() != null) {
            Template template = templateRepository.findByIdAndUserId(dto.getTemplateId(), userId)
                    .orElseThrow(() -> new RuntimeException("Template not found or access denied"));
            existing.setTemplate(template);
        }

        if (dto.getCvVariantId() != null) {
            CvVariant cvVariant = cvVariantRepository.findByIdAndUserId(dto.getCvVariantId(), userId)
                    .orElseThrow(() -> new RuntimeException("CV Variant not found or access denied"));
            existing.setCvVariant(cvVariant);
        }

        if (dto.getSkillIds() != null) {
            existing.setSkills(resolveSkills(dto.getSkillIds(), userId));
        }

        return toDto(presetRepository.save(existing));
    }

    @Transactional(readOnly = true)
    public Page<ApplicationPresetDto> getAllForUser(Long userId, String keyword, String language, Pageable pageable) {
        String keywordParam = (keyword != null && !keyword.isBlank()) ? keyword : null;
        String languageParam = (language != null && !language.isBlank()) ? language : null;

        return presetRepository.findByUserIdWithFilters(userId, keywordParam, languageParam, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ApplicationPresetDto getByIdAndUser(Long id, Long userId) {
        ApplicationPreset preset = presetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Preset not found or access denied"));
        return toDto(preset);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        ApplicationPreset preset = presetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Preset not found or access denied"));
        presetRepository.delete(preset);
    }

    private Set<Skill> resolveSkills(Set<Long> skillIds, Long userId) {
        Set<Skill> skills = new HashSet<>();
        if (skillIds == null)
            return skills;
        for (Long skillId : skillIds) {
            skills.add(skillRepository.findByIdAndUserId(skillId, userId)
                    .orElseThrow(() -> new RuntimeException("Skill not found or access denied: " + skillId)));
        }
        return skills;
    }

    private ApplicationPresetDto toDto(ApplicationPreset p) {
        return ApplicationPresetDto.builder()
                .id(p.getId())
                .name(p.getName())
                .jobTitle(p.getJobTitle())
                .language(p.getLanguage())
                .templateId(p.getTemplate() != null ? p.getTemplate().getId() : null)
                .cvVariantId(p.getCvVariant() != null ? p.getCvVariant().getId() : null)
                .skillIds(p.getSkills() != null
                        ? p.getSkills().stream()
                                .filter(skill -> skill != null)
                                .map(skill -> skill.getId())
                                .collect(Collectors.toSet())
                        : Set.of())
                .notes(p.getNotes())
                .build();
    }
}
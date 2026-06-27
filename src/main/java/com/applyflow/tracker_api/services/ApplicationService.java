package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.dtos.ApplicationCreateDto;
import com.applyflow.tracker_api.models.*;
import com.applyflow.tracker_api.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final TemplateRepository templateRepository;
    private final CvVariantRepository cvVariantRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;

    @Transactional
    public Application createAndCompileApplication(ApplicationCreateDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + dto.getUserId()));

        Template template = templateRepository.findByIdAndUserId(dto.getTemplateId(), dto.getUserId())
                .orElseThrow(() -> new RuntimeException(
                        "Template not found or access denied for id: " + dto.getTemplateId()));

        CvVariant cvVariant = cvVariantRepository.findByIdAndUserId(dto.getCvVariantId(), dto.getUserId())
                .orElseThrow(() -> new RuntimeException(
                        "CV Variant not found or access denied for id: " + dto.getCvVariantId()));

        Set<Skill> selectedSkills = new HashSet<>();
        for (Long skillId : dto.getSkillIds()) {
            Skill skill = skillRepository.findByIdAndUserId(skillId, dto.getUserId())
                    .orElseThrow(() -> new RuntimeException("Skill not found or access denied for id: " + skillId));
            selectedSkills.add(skill);
        }

        String compiledSubject = template.getSubjectTemplate()
                .replace("{{position}}", dto.getJobTitle())
                .replace("{{role}}", dto.getJobTitle())
                .replace("{{company}}", dto.getCompanyName());

        StringBuilder skillsBulletPoints = new StringBuilder();
        for (Skill skill : selectedSkills) {
            skillsBulletPoints.append("• ").append(skill.getName()).append(" : ");
            if ("fr".equalsIgnoreCase(dto.getLanguage())) {
                skillsBulletPoints.append(skill.getSentenceFr());
            } else {
                skillsBulletPoints.append(skill.getSentenceEn());
            }
            skillsBulletPoints.append("\n");
        }

        String skillsContent = skillsBulletPoints.toString().trim();
        String bodyTemplate = template.getBodyTemplate();

        if (!bodyTemplate.contains("{{skills_block}}") && !skillsContent.isEmpty()) {
            bodyTemplate += "\n\n{{skills_block}}";
        }

        String compiledBody = bodyTemplate
                .replace("{{position}}", dto.getJobTitle())
                .replace("{{role}}", dto.getJobTitle())
                .replace("{{company}}", dto.getCompanyName())
                .replace("{{skills_block}}", skillsContent);

        Application application = Application.builder()
                .companyName(dto.getCompanyName())
                .jobTitle(dto.getJobTitle())
                .recipientEmail(dto.getRecipientEmail())
                .language(dto.getLanguage())
                .generatedSubject(compiledSubject)
                .generatedBody(compiledBody)
                .status("Compiled")
                .template(template)
                .cvVariant(cvVariant)
                .user(user)
                .skills(selectedSkills)
                .notes(dto.getNotes())
                .build();

        return applicationRepository.save(application);
    }

    public Page<Application> getAllApplicationsForUser(
            Long userId, String status, String keyword, Pageable pageable) {

        boolean hasStatus = status != null && !status.isBlank();
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        if (hasStatus && hasKeyword) {
            return applicationRepository.searchByKeywordAndStatus(userId, keyword, status, pageable);
        } else if (hasStatus) {
            return applicationRepository.findByUserIdAndStatusIgnoreCase(userId, status, pageable);
        } else if (hasKeyword) {
            return applicationRepository.searchByKeyword(userId, keyword, pageable);
        } else {
            return applicationRepository.findByUserId(userId, pageable);
        }
    }

    public Application getApplicationByIdAndUser(Long id, Long userId) {
        return applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Application tracking record not found or access denied."));
    }

    @Transactional
    public Application updateApplicationStatusOrNotes(Long id, Long userId, String status, String notes) {
        Application existing = getApplicationByIdAndUser(id, userId);
        if (status != null)
            existing.setStatus(status);
        if (notes != null)
            existing.setNotes(notes);
        return applicationRepository.save(existing);
    }

    @Transactional
    public void deleteApplication(Long id, Long userId) {
        Application app = getApplicationByIdAndUser(id, userId);
        applicationRepository.delete(app);
    }
}
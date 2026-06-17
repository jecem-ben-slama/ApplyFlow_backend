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
import java.util.List;
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
        // 1. Validate and fetch all required dependencies
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + dto.getUserId()));

        Template template = templateRepository.findById(dto.getTemplateId())
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + dto.getTemplateId()));

        CvVariant cvVariant = cvVariantRepository.findById(dto.getCvVariantId())
                .orElseThrow(() -> new RuntimeException("CV Variant not found with id: " + dto.getCvVariantId()));

        List<Skill> skillsList = skillRepository.findAllById(dto.getSkillIds());
        Set<Skill> selectedSkills = new HashSet<>(skillsList);

        // 2. Stitch the Subject Line Template (Supports both {{position}} and {{role}})
        String compiledSubject = template.getSubjectTemplate()
                .replace("{{position}}", dto.getJobTitle())
                .replace("{{role}}", dto.getJobTitle())
                .replace("{{company}}", dto.getCompanyName());

        // 3. Construct the customized bullet points block
        StringBuilder skillsBulletPoints = new StringBuilder();
        for (Skill skill : selectedSkills) {
            skillsBulletPoints.append("• ").append(skill.getDisplayName()).append(" : ");
            if ("fr".equalsIgnoreCase(dto.getLanguage())) {
                skillsBulletPoints.append(skill.getSentenceFr());
            } else {
                skillsBulletPoints.append(skill.getSentenceEn());
            }
            skillsBulletPoints.append("\n");
        }

        // 4. Stitch the Core Email Body (Handles flexible tags + safe skills rendering)
        String skillsContent = skillsBulletPoints.toString().trim();
        String bodyTemplate = template.getBodyTemplate();

        // If the template doesn't explicitly contain the block placeholder, append it
        // to the bottom
        if (!bodyTemplate.contains("{{skills_block}}") && !skillsContent.isEmpty()) {
            bodyTemplate += "\n\n{{skills_block}}";
        }

        String compiledBody = bodyTemplate
                .replace("{{position}}", dto.getJobTitle())
                .replace("{{role}}", dto.getJobTitle())
                .replace("{{company}}", dto.getCompanyName())
                .replace("{{skills_block}}", skillsContent);

        // 5. Build and save the application persistence layer entry
        Application application = Application.builder()
                .companyName(dto.getCompanyName())
                .jobTitle(dto.getJobTitle())
                .recipientEmail(dto.getRecipientEmail())
                .language(dto.getLanguage())
                .generatedSubject(compiledSubject)
                .generatedBody(compiledBody)
                .template(template)
                .cvVariant(cvVariant)
                .user(user)
                .skills(selectedSkills)
                .notes(dto.getNotes())
                .build();

        return applicationRepository.save(application);
    }

    public Page<Application> getAllApplications(Pageable pageable) {
        return applicationRepository.findAll(pageable);
    }

    public Application getApplicationById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application tracking record not found with id: " + id));
    }

    @Transactional
    public Application updateApplicationStatusOrNotes(Long id, String status, String notes) {
        Application existing = getApplicationById(id);
        if (status != null)
            existing.setStatus(status);
        if (notes != null)
            existing.setNotes(notes);
        return applicationRepository.save(existing);
    }

    public void deleteApplication(Long id) {
        Application app = getApplicationById(id);
        applicationRepository.delete(app);
    }
}
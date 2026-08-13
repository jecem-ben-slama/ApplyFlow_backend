package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.dtos.ApplicationCreateDto;
import com.applyflow.tracker_api.dtos.ApplicationResponseDto;
import com.applyflow.tracker_api.models.*;
import com.applyflow.tracker_api.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final TemplateRepository templateRepository;
    private final CvVariantRepository cvVariantRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final ApplicationEventService applicationEventService;

    @Transactional
    public ApplicationResponseDto createAndCompileApplication(ApplicationCreateDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + dto.getUserId()));

        Template template = templateRepository.findByIdAndUserId(dto.getTemplateId(), dto.getUserId())
                .orElseThrow(() -> new RuntimeException(
                        "Template not found or access denied for id: " + dto.getTemplateId()));

        CvVariant cvVariant = null;
        if (dto.getCvVariantId() != null) {
            cvVariant = cvVariantRepository.findByIdAndUserId(dto.getCvVariantId(), dto.getUserId())
                    .orElseThrow(() -> new RuntimeException(
                            "CV Variant not found or access denied for id: " + dto.getCvVariantId()));
        }

        Set<Skill> selectedSkills = new HashSet<>();
        for (Long skillId : dto.getSkillIds()) {
            Skill skill = skillRepository.findByIdAndUserId(skillId, dto.getUserId())
                    .orElseThrow(() -> new RuntimeException("Skill not found or access denied for id: " + skillId));
            selectedSkills.add(skill);
        }

        String compiledSubject = template.getSubjectTemplate()
                .replace("{{position}}", dto.getJobTitle())
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
                .replace("{{company}}", dto.getCompanyName())
                .replace("{{skills_block}}", skillsContent);

        Application application = Application.builder()
                .companyName(dto.getCompanyName())
                .jobTitle(dto.getJobTitle())
                .recipientEmail(dto.getRecipientEmail())
                .language(dto.getLanguage())
                .generatedSubject(compiledSubject)
                .generatedBody(compiledBody)
                .status(ApplicationStatus.COMPILED.name())
                .template(template)
                .cvVariant(cvVariant)
                .user(user)
                .skills(selectedSkills)
                .notes(dto.getNotes())
                .build();

        log.info("Compiling fresh application tracking context for company: {}", application.getCompanyName());

        Application saved = applicationRepository.save(application);
        applicationEventService.recordEvent(saved, ApplicationStatus.COMPILED.name(), "Application compiled");

        return convertToDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponseDto> getAllApplicationsForUser(
            Long userId, String status, String keyword, String language, Pageable pageable) {

        String statusParam = (status != null && !status.isBlank()) ? status : null;
        String keywordParam = (keyword != null && !keyword.isBlank()) ? keyword : null;
        String languageParam = (language != null && !language.isBlank()) ? language : null;

        Page<Application> applicationsPage = applicationRepository.findByUserIdWithFilters(
                userId, statusParam, keywordParam, languageParam, pageable);

        return applicationsPage.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public ApplicationResponseDto getApplicationByIdAndUser(Long id, Long userId) {
        Application app = applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Application tracking record not found or access denied."));
        return convertToDto(app);
    }

    @Transactional
    public ApplicationResponseDto updateApplicationStatusOrNotes(Long id, Long userId, String status, String notes) {
        Application existing = applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Application tracking record not found or access denied."));

        if (status != null) {
            if (!ApplicationStatus.isValid(status)) {
                throw new IllegalArgumentException("Invalid status: " + status);
            }

            String normalized = status.toUpperCase();
            String oldStatus = existing.getStatus();

            if (!applicationEventService.shouldTransition(oldStatus, normalized)) {
                throw new IllegalStateException("Invalid status transition from " + oldStatus + " to " + normalized);
            }

            existing.setStatus(normalized);
            applicationEventService.recordEvent(existing, normalized, null);
        }

        if (notes != null) {
            existing.setNotes(notes);
        }

        Application updated = applicationRepository.save(existing);
        return convertToDto(updated);
    }

    /**
     * For system-triggered status transitions (email send confirmation,
     * open-tracking pixel) that have no authenticated user on the request.
     * Delegates the forward-only ordering check to ApplicationEventService so
     * a stale event (e.g. a re-opened email) can't regress a status that has
     * already moved further along the funnel.
     */
    @Transactional
    public void recordSystemStatusEvent(Long applicationId, ApplicationStatus status, String note) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        String normalized = status.name();
        String oldStatus = app.getStatus();

        if (applicationEventService.shouldTransition(oldStatus, normalized)) {
            app.setStatus(normalized);
            applicationRepository.save(app);
            applicationEventService.recordEvent(app, normalized, note);
        }
    }

    @Transactional
    public void deleteApplication(Long id, Long userId) {
        Application app = applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Application tracking record not found or access denied."));
        applicationRepository.delete(app);
    }

    private ApplicationResponseDto convertToDto(Application app) {
        return ApplicationResponseDto.builder()
                .id(app.getId())
                .companyName(app.getCompanyName())
                .jobTitle(app.getJobTitle())
                .recipientEmail(app.getRecipientEmail())
                .language(app.getLanguage())
                .status(app.getStatus())
                .generatedSubject(app.getGeneratedSubject())
                .generatedBody(app.getGeneratedBody())
                .dateApplied(app.getDateApplied())
                .notes(app.getNotes())
                .templateId(app.getTemplate() != null ? app.getTemplate().getId() : null)
                .cvVariantId(app.getCvVariant() != null ? app.getCvVariant().getId() : null)
                .userId(app.getUser() != null ? app.getUser().getId() : null)
                .skillIds(app.getSkills() != null
                        ? app.getSkills().stream().filter(Objects::nonNull).map(skill -> skill.getId())
                                .collect(Collectors.toSet())
                        : Collections.emptySet())
                .build();
    }
}
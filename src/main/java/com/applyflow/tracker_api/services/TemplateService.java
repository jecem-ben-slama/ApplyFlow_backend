package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.models.Template;
import com.applyflow.tracker_api.repositories.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;

    public Template createTemplate(Template template) {
        return templateRepository.save(template);
    }

    public Page<Template> getTemplatesForUser(Long userId, Pageable pageable) {
        return templateRepository.findByUserId(userId, pageable);
    }

    public Page<Template> getTemplatesForUserByLanguage(Long userId, String language, Pageable pageable) {
        return templateRepository.findByUserIdAndLanguage(userId, language, pageable);
    }

    public Page<Template> searchTemplatesForUser(Long userId, String term, Pageable pageable) {
        return templateRepository.searchByUserIdAndTerm(userId, term, pageable);
    }

    public Page<Template> searchTemplatesForUserByLanguage(
            Long userId, String language, String term, Pageable pageable) {
        return templateRepository.searchByUserIdAndLanguageAndTerm(userId, language, term, pageable);
    }

    public Template getTemplateByIdAndUser(Long id, Long userId) {
        return templateRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Template not found or access denied."));
    }

    @Transactional
    public Template updateTemplate(Long id, Long userId, Template templateDetails) {
        Template existingTemplate = getTemplateByIdAndUser(id, userId);

        existingTemplate.setName(templateDetails.getName());
        existingTemplate.setLanguage(templateDetails.getLanguage());
        existingTemplate.setSubjectTemplate(templateDetails.getSubjectTemplate());
        existingTemplate.setBodyTemplate(templateDetails.getBodyTemplate());
        existingTemplate.setUpdatedAt(LocalDateTime.now());

        return templateRepository.save(existingTemplate);
    }

    @Transactional
    public void deleteTemplate(Long id, Long userId) {
        Template template = getTemplateByIdAndUser(id, userId);
        templateRepository.delete(template);
    }
}
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

    public Page<Template> getAllTemplates(Pageable pageable) {
        return templateRepository.findAll(pageable);
    }

    public Template getTemplateById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));
    }

    @Transactional
    public Template updateTemplate(Long id, Template templateDetails) {
        Template existingTemplate = getTemplateById(id);

        existingTemplate.setName(templateDetails.getName());
        existingTemplate.setLanguage(templateDetails.getLanguage());
        existingTemplate.setTier(templateDetails.getTier());
        existingTemplate.setSubjectTemplate(templateDetails.getSubjectTemplate());
        existingTemplate.setBodyTemplate(templateDetails.getBodyTemplate());
        existingTemplate.setUpdatedAt(LocalDateTime.now());

        return templateRepository.save(existingTemplate);
    }

    public void deleteTemplate(Long id) {
        Template template = getTemplateById(id);
        templateRepository.delete(template);
    }
}
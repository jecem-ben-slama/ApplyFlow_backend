package com.applyflow.tracker_api.controllers;

import com.applyflow.tracker_api.dtos.ApiResponse;
import com.applyflow.tracker_api.dtos.TemplateDto;
import com.applyflow.tracker_api.models.Template;
import com.applyflow.tracker_api.services.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    public ResponseEntity<ApiResponse<TemplateDto>> createTemplate(@RequestBody TemplateDto dto) {
        Template entity = Template.builder()
                .name(dto.getName())
                .language(dto.getLanguage())
                .tier(dto.getTier())
                .subjectTemplate(dto.getSubjectTemplate())
                .bodyTemplate(dto.getBodyTemplate())
                .build();

        Template saved = templateService.createTemplate(entity);
        return new ResponseEntity<>(
                ApiResponse.success("Template created successfully", convertToDto(saved)),
                HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TemplateDto>>> getAllTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TemplateDto> pagedDtos = templateService.getAllTemplates(pageable).map(this::convertToDto);
        return ResponseEntity.ok(ApiResponse.success("Templates retrieved successfully", pagedDtos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TemplateDto>> getTemplateById(@PathVariable Long id) {
        Template template = templateService.getTemplateById(id);
        return ResponseEntity.ok(ApiResponse.success("Template retrieved successfully", convertToDto(template)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TemplateDto>> updateTemplate(@PathVariable Long id,
            @RequestBody TemplateDto dto) {
        Template details = Template.builder()
                .name(dto.getName())
                .language(dto.getLanguage())
                .tier(dto.getTier())
                .subjectTemplate(dto.getSubjectTemplate())
                .bodyTemplate(dto.getBodyTemplate())
                .build();

        Template updated = templateService.updateTemplate(id, details);
        return ResponseEntity.ok(ApiResponse.success("Template updated successfully", convertToDto(updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Template deleted successfully"));
    }

    private TemplateDto convertToDto(Template template) {
        return TemplateDto.builder()
                .id(template.getId())
                .name(template.getName())
                .language(template.getLanguage())
                .tier(template.getTier())
                .subjectTemplate(template.getSubjectTemplate())
                .bodyTemplate(template.getBodyTemplate())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
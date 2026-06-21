package com.applyflow.tracker_api.controllers;

import com.applyflow.tracker_api.config.CustomOAuth2User;
import com.applyflow.tracker_api.dtos.ApiResponse;
import com.applyflow.tracker_api.dtos.ApplicationCreateDto;
import com.applyflow.tracker_api.dtos.ApplicationResponseDto;
import com.applyflow.tracker_api.models.Application;
import com.applyflow.tracker_api.models.Skill;
import com.applyflow.tracker_api.services.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationResponseDto>> createApplication(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody ApplicationCreateDto dto) {

        // Force the active user's ID into the creation DTO for processing safety
        dto.setUserId(principal.getId());

        Application compiled = applicationService.createAndCompileApplication(dto);
        return new ResponseEntity<>(
                ApiResponse.success("Application created and email compiled successfully", convertToDto(compiled)),
                HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ApplicationResponseDto>>> getAllApplications(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateApplied") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ApplicationResponseDto> responseData = applicationService
                .getAllApplicationsForUser(principal.getId(), pageable)
                .map(this::convertToDto);

        return ResponseEntity.ok(ApiResponse.success("User tracking history retrieved successfully", responseData));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationResponseDto>> getApplicationById(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long id) {
        Application app = applicationService.getApplicationByIdAndUser(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Application details retrieved successfully", convertToDto(app)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationResponseDto>> patchApplicationStatusOrNotes(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String notes) {

        Application updated = applicationService.updateApplicationStatusOrNotes(id, principal.getId(), status, notes);
        return ResponseEntity.ok(ApiResponse.success("Application state modified successfully", convertToDto(updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteApplication(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long id) {
        applicationService.deleteApplication(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Application tracking record deleted successfully"));
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
                .skillIds(app.getSkills().stream().map(Skill::getId).collect(Collectors.toSet()))
                .build();
    }
}
package com.applyflow.tracker_api.controllers;

import com.applyflow.tracker_api.config.CustomOAuth2User;
import com.applyflow.tracker_api.dtos.ApiResponse;
import com.applyflow.tracker_api.dtos.CvVariantDto;
import com.applyflow.tracker_api.models.CvVariant;
import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.services.CvVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cv-variants")
@RequiredArgsConstructor
public class CvVariantController {

    private final CvVariantService cvVariantService;

    @PostMapping
    public ResponseEntity<ApiResponse<CvVariantDto>> createCvVariant(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody CvVariantDto dto) {

        CvVariant entity = CvVariant.builder()
                .name(dto.getName())
                .language(dto.getLanguage())
                .fileUrl(dto.getFileUrl())
                .user(User.builder().id(principal.getId()).build())
                .build();

        CvVariant saved = cvVariantService.createCvVariant(entity);
        return new ResponseEntity<>(
                ApiResponse.success("CV Variant created successfully", convertToDto(saved)),
                HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CvVariantDto>>> getAllCvVariants(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestParam(required = false) String language) {

        List<CvVariant> cvList = (language != null)
                ? cvVariantService.getCvVariantsForUserByLanguage(principal.getId(), language)
                : cvVariantService.getCvVariantsForUser(principal.getId());

        List<CvVariantDto> dtos = cvList.stream().map(this::convertToDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("CV Variants retrieved successfully", dtos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CvVariantDto>> getCvVariantById(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long id) {
        CvVariant cv = cvVariantService.getCvVariantByIdAndUser(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("CV Variant retrieved successfully", convertToDto(cv)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CvVariantDto>> updateCvVariant(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long id,
            @RequestBody CvVariantDto dto) {

        CvVariant details = CvVariant.builder()
                .name(dto.getName())
                .language(dto.getLanguage())
                .fileUrl(dto.getFileUrl())
                .build();

        CvVariant updated = cvVariantService.updateCvVariant(id, principal.getId(), details);
        return ResponseEntity.ok(ApiResponse.success("CV Variant updated successfully", convertToDto(updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCvVariant(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long id) {
        cvVariantService.deleteCvVariant(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("CV Variant deleted successfully"));
    }

    private CvVariantDto convertToDto(CvVariant cv) {
        return CvVariantDto.builder()
                .id(cv.getId())
                .name(cv.getName())
                .language(cv.getLanguage())
                .fileUrl(cv.getFileUrl())
                .createdAt(cv.getCreatedAt())
                .userId(cv.getUser() != null ? cv.getUser().getId() : null)
                .build();
    }
}
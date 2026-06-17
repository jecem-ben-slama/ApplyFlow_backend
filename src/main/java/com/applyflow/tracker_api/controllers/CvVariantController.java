package com.applyflow.tracker_api.controllers;

import com.applyflow.tracker_api.dtos.ApiResponse;
import com.applyflow.tracker_api.dtos.CvVariantDto;
import com.applyflow.tracker_api.models.CvVariant;
import com.applyflow.tracker_api.services.CvVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cv-variants")
@RequiredArgsConstructor
public class CvVariantController {

    private final CvVariantService cvVariantService;

    @PostMapping
    public ResponseEntity<ApiResponse<CvVariantDto>> createCvVariant(@RequestBody CvVariantDto dto) {
        CvVariant entity = CvVariant.builder()
                .name(dto.getName())
                .language(dto.getLanguage())
                .fileUrl(dto.getFileUrl())
                .build();

        CvVariant saved = cvVariantService.createCvVariant(entity);
        return new ResponseEntity<>(
                ApiResponse.success("CV Variant created successfully", convertToDto(saved)),
                HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CvVariantDto>>> getAllCvVariants() {
        List<CvVariantDto> dtos = cvVariantService.getAllCvVariants().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("CV Variants retrieved successfully", dtos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CvVariantDto>> getCvVariantById(@PathVariable Long id) {
        CvVariant cv = cvVariantService.getCvVariantById(id);
        return ResponseEntity.ok(ApiResponse.success("CV Variant retrieved successfully", convertToDto(cv)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CvVariantDto>> updateCvVariant(@PathVariable Long id,
            @RequestBody CvVariantDto dto) {
        CvVariant details = CvVariant.builder()
                .name(dto.getName())
                .language(dto.getLanguage())
                .fileUrl(dto.getFileUrl())
                .build();

        CvVariant updated = cvVariantService.updateCvVariant(id, details);
        return ResponseEntity.ok(ApiResponse.success("CV Variant updated successfully", convertToDto(updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCvVariant(@PathVariable Long id) {
        cvVariantService.deleteCvVariant(id);
        return ResponseEntity.ok(ApiResponse.success("CV Variant deleted successfully"));
    }

    // Helper method to transform Domain Entities into clean Data Transfer Objects
    private CvVariantDto convertToDto(CvVariant cv) {
        return CvVariantDto.builder()
                .id(cv.getId())
                .name(cv.getName())
                .language(cv.getLanguage())
                .fileUrl(cv.getFileUrl())
                .createdAt(cv.getCreatedAt())
                .build();
    }
}
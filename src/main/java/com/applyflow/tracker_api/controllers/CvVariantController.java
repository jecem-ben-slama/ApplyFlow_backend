package com.applyflow.tracker_api.controllers;

import com.applyflow.tracker_api.config.SecurityContextService;
import com.applyflow.tracker_api.dtos.ApiResponse;
import com.applyflow.tracker_api.dtos.CvVariantDto;
import com.applyflow.tracker_api.models.CvVariant;
import com.applyflow.tracker_api.models.User;
import com.applyflow.tracker_api.services.CvVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cv-variants")
@RequiredArgsConstructor
public class CvVariantController {

        private final CvVariantService cvVariantService;
        private final SecurityContextService securityContextService;

        @PostMapping
        public ResponseEntity<ApiResponse<CvVariantDto>> createCvVariant(@RequestBody CvVariantDto dto) {
                Long userId = securityContextService.getCurrentUserId();

                CvVariant entity = CvVariant.builder()
                                .name(dto.getName())
                                .language(dto.getLanguage())
                                .fileUrl(dto.getFileUrl())
                                .user(User.builder().id(userId).build())
                                .build();

                CvVariant saved = cvVariantService.createCvVariant(entity);
                return new ResponseEntity<>(
                                ApiResponse.success("CV Variant created successfully", convertToDto(saved)),
                                HttpStatus.CREATED);
        }

        @GetMapping
        @Transactional(readOnly = true)
        public ResponseEntity<ApiResponse<Page<CvVariantDto>>> getAllCvVariants(
                        @RequestParam(required = false) String language,
                        @RequestParam(required = false) String search,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id") String sortBy,
                        @RequestParam(defaultValue = "asc") String direction) {

                Long userId = securityContextService.getCurrentUserId();

                Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending()
                                : Sort.by(sortBy).ascending();
                Pageable pageable = PageRequest.of(page, size, sort);

                Page<CvVariant> cvPage = cvVariantService.getCvVariantsForUser(userId, language, search, pageable);

                Page<CvVariantDto> dtos = cvPage.map(this::convertToDto);
                return ResponseEntity.ok(ApiResponse.success("CV Variants retrieved successfully", dtos));
        }
        @GetMapping("/{id}")
        @Transactional(readOnly = true)
        public ResponseEntity<ApiResponse<CvVariantDto>> getCvVariantById(@PathVariable Long id) {
                Long userId = securityContextService.getCurrentUserId();
                CvVariant cv = cvVariantService.getCvVariantByIdAndUser(id, userId);
                return ResponseEntity.ok(ApiResponse.success("CV Variant retrieved successfully", convertToDto(cv)));
        }

        @PutMapping("/{id}")
        public ResponseEntity<ApiResponse<CvVariantDto>> updateCvVariant(
                        @PathVariable Long id,
                        @RequestBody CvVariantDto dto) {

                Long userId = securityContextService.getCurrentUserId();

                CvVariant details = CvVariant.builder()
                                .name(dto.getName())
                                .language(dto.getLanguage())
                                .fileUrl(dto.getFileUrl())
                                .build();

                CvVariant updated = cvVariantService.updateCvVariant(id, userId, details);
                return ResponseEntity.ok(ApiResponse.success("CV Variant updated successfully", convertToDto(updated)));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse<Void>> deleteCvVariant(@PathVariable Long id) {
                Long userId = securityContextService.getCurrentUserId();
                cvVariantService.deleteCvVariant(id, userId);
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
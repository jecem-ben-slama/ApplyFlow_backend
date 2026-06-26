package com.applyflow.tracker_api.controllers;

import com.applyflow.tracker_api.config.SecurityContextService;
import com.applyflow.tracker_api.dtos.ApiResponse;
import com.applyflow.tracker_api.dtos.CategoryDto;
import com.applyflow.tracker_api.models.Category;
import com.applyflow.tracker_api.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CategoryController {

    private final CategoryService categoryService;
    private final SecurityContextService securityContextService;

    // ── GET /api/categories ──────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getAll() {
        Long userId = securityContextService.getCurrentUserId();
        List<CategoryDto> categories = categoryService.getAllCategories(userId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully.", categories));
    }

    // ── GET /api/categories/{id} ─────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDto>> getById(@PathVariable Long id) {
        Long userId = securityContextService.getCurrentUserId();
        Category category = categoryService.getCategoryById(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Category retrieved successfully.", convertToDto(category)));
    }

    // ── POST /api/categories ─────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryDto>> create(@RequestBody Map<String, String> body) {
        Long userId = securityContextService.getCurrentUserId();
        Category created = categoryService.createCategory(userId, body.get("name"));
        return new ResponseEntity<>(
                ApiResponse.success("Category created successfully.", convertToDto(created)),
                HttpStatus.CREATED);
    }

    // ── PUT /api/categories/{id} ─────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDto>> update(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        Long userId = securityContextService.getCurrentUserId();
        Category updated = categoryService.updateCategory(id, userId, body.get("name"));
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully.", convertToDto(updated)));
    }

    // ── DELETE /api/categories/{id} ──────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Long userId = securityContextService.getCurrentUserId();
        categoryService.deleteCategory(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully."));
    }

    private CategoryDto convertToDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .userId(category.getUser() != null ? category.getUser().getId() : null)
                .skillCount(category.getSkills() != null ? category.getSkills().size() : 0)
                .build();
    }
}
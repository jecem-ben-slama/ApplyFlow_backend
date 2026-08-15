package com.applyflow.tracker_api.controllers;

import com.applyflow.tracker_api.config.SecurityContextService;
import com.applyflow.tracker_api.dtos.ApiResponse;
import com.applyflow.tracker_api.dtos.ApplicationPresetDto;
import com.applyflow.tracker_api.services.ApplicationPresetService;
import com.applyflow.tracker_api.utils.SortValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/application-presets")
@RequiredArgsConstructor
public class ApplicationPresetController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "jobTitle", "language", "id");

    private final ApplicationPresetService presetService;
    private final SecurityContextService securityContextService;

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationPresetDto>> create(@RequestBody ApplicationPresetDto dto) {
        Long userId = securityContextService.getCurrentUserId();
        ApplicationPresetDto response = presetService.create(dto, userId);
        return new ResponseEntity<>(ApiResponse.success("Preset saved successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ApplicationPresetDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String language) {

        Long userId = securityContextService.getCurrentUserId();

        Sort sort = SortValidation.resolve(sortBy, direction, ALLOWED_SORT_FIELDS);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ApplicationPresetDto> results = presetService.getAllForUser(userId, keyword, language, pageable);
        return ResponseEntity.ok(ApiResponse.success("Presets retrieved successfully", results));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationPresetDto>> getById(@PathVariable Long id) {
        Long userId = securityContextService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Preset retrieved successfully",
                presetService.getByIdAndUser(id, userId)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationPresetDto>> update(
            @PathVariable Long id, @RequestBody ApplicationPresetDto dto) {
        Long userId = securityContextService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Preset updated successfully",
                presetService.update(id, dto, userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Long userId = securityContextService.getCurrentUserId();
        presetService.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Preset deleted successfully"));
    }
}
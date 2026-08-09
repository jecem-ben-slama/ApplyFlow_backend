package com.applyflow.tracker_api.controllers;

import com.applyflow.tracker_api.config.SecurityContextService;
import com.applyflow.tracker_api.dtos.ApiResponse;
import com.applyflow.tracker_api.dtos.ApplicationSummaryDto;
import com.applyflow.tracker_api.dtos.StatMetricDto;
import com.applyflow.tracker_api.services.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final SecurityContextService securityContextService;

    @GetMapping("/cv-performance")
    public ResponseEntity<ApiResponse<List<StatMetricDto>>> getCvStats(
            @RequestParam(required = false) List<String> successStatuses,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long userId = securityContextService.getCurrentUserId();
        List<StatMetricDto> stats = analyticsService.getCvVariantStats(userId, successStatuses,
                toStartOfDay(from), toEndOfDay(to));
        return ResponseEntity.ok(ApiResponse.success("CV variant performance metrics retrieved successfully", stats));
    }

    @GetMapping("/language-performance")
    public ResponseEntity<ApiResponse<List<StatMetricDto>>> getLanguageStats(
            @RequestParam(required = false) List<String> successStatuses,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long userId = securityContextService.getCurrentUserId();
        List<StatMetricDto> stats = analyticsService.getLanguageStats(userId, successStatuses,
                toStartOfDay(from), toEndOfDay(to));
        return ResponseEntity.ok(ApiResponse.success("Language performance metrics retrieved successfully", stats));
    }

    @GetMapping("/job-performance")
    public ResponseEntity<ApiResponse<List<StatMetricDto>>> getJobStats(
            @RequestParam(required = false) List<String> successStatuses,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long userId = securityContextService.getCurrentUserId();
        List<StatMetricDto> stats = analyticsService.getJobTitleStats(userId, successStatuses,
                toStartOfDay(from), toEndOfDay(to));
        return ResponseEntity.ok(ApiResponse.success("Job post performance metrics retrieved successfully", stats));
    }

    @GetMapping("/template-performance")
    public ResponseEntity<ApiResponse<List<StatMetricDto>>> getTemplateStats(
            @RequestParam(required = false) List<String> successStatuses,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long userId = securityContextService.getCurrentUserId();
        List<StatMetricDto> stats = analyticsService.getTemplateStats(userId, successStatuses,
                toStartOfDay(from), toEndOfDay(to));
        return ResponseEntity.ok(ApiResponse.success("Template performance metrics retrieved successfully", stats));
    }

    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<List<ApplicationSummaryDto>>> listApplications() {
        Long userId = securityContextService.getCurrentUserId();
        List<ApplicationSummaryDto> response = analyticsService.listApplicationSummaries(userId);
        return ResponseEntity.ok(ApiResponse.success("Applications retrieved successfully", response));
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    private LocalDateTime toEndOfDay(LocalDate date) {
        return date != null ? date.atTime(23, 59, 59) : null;
    }
}
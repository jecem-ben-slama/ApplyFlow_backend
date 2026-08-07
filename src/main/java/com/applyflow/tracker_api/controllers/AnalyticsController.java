package com.applyflow.tracker_api.controllers;

import com.applyflow.tracker_api.config.SecurityContextService;
import com.applyflow.tracker_api.dtos.ApiResponse;
import com.applyflow.tracker_api.dtos.StatMetricDto;
import com.applyflow.tracker_api.services.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final SecurityContextService securityContextService;

    @GetMapping("/cv-performance")
    public ResponseEntity<ApiResponse<List<StatMetricDto>>> getCvStats() {
        Long userId = securityContextService.getCurrentUserId();
        List<StatMetricDto> stats = analyticsService.getCvVariantStats(userId);
        return ResponseEntity.ok(ApiResponse.success("CV variant performance metrics retrieved successfully", stats));
    }

    @GetMapping("/language-performance")
    public ResponseEntity<ApiResponse<List<StatMetricDto>>> getLanguageStats() {
        Long userId = securityContextService.getCurrentUserId();
        List<StatMetricDto> stats = analyticsService.getLanguageStats(userId);
        return ResponseEntity.ok(ApiResponse.success("Language performance metrics retrieved successfully", stats));
    }

    @GetMapping("/job-performance")
    public ResponseEntity<ApiResponse<List<StatMetricDto>>> getJobStats() {
        Long userId = securityContextService.getCurrentUserId();
        List<StatMetricDto> stats = analyticsService.getJobTitleStats(userId);
        return ResponseEntity.ok(ApiResponse.success("Job post performance metrics retrieved successfully", stats));
    }
}
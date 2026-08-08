package com.applyflow.tracker_api.controllers;

import com.applyflow.tracker_api.config.SecurityContextService;
import com.applyflow.tracker_api.dtos.ApiResponse;
import com.applyflow.tracker_api.dtos.FunnelStageDto;
import com.applyflow.tracker_api.dtos.StatsSummaryDto;
import com.applyflow.tracker_api.dtos.TimelineEventDto;
import com.applyflow.tracker_api.services.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final SecurityContextService securityContextService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<StatsSummaryDto>> getSummary() {
        Long userId = securityContextService.getCurrentUserId();
        StatsSummaryDto response = statsService.getSummary(userId);
        return ResponseEntity.ok(ApiResponse.success("Stats summary retrieved successfully", response));
    }

    @GetMapping("/funnel")
    public ResponseEntity<ApiResponse<List<FunnelStageDto>>> getFunnel() {
        Long userId = securityContextService.getCurrentUserId();
        List<FunnelStageDto> response = statsService.getFunnel(userId);
        return ResponseEntity.ok(ApiResponse.success("Funnel data retrieved successfully", response));
    }
    
    @GetMapping("/{id}/timeline")
    public ResponseEntity<ApiResponse<List<TimelineEventDto>>> getApplicationTimeline(@PathVariable Long id) {
        Long userId = securityContextService.getCurrentUserId();
        List<TimelineEventDto> response = statsService.getApplicationTimeline(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Application timeline retrieved successfully", response));
    }

    @GetMapping("/recent-events")
    public ResponseEntity<ApiResponse<List<TimelineEventDto>>> getRecentEvents(
            @RequestParam(defaultValue = "8") int limit) {
        Long userId = securityContextService.getCurrentUserId();
        List<TimelineEventDto> response = statsService.getRecentEvents(userId, limit);
        return ResponseEntity.ok(ApiResponse.success("Recent activity retrieved successfully", response));
    }
}
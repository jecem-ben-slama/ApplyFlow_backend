package com.applyflow.tracker_api.controllers;

import com.applyflow.tracker_api.config.SecurityContextService;
import com.applyflow.tracker_api.dtos.*;
import com.applyflow.tracker_api.services.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final SecurityContextService securityContextService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<StatsSummaryDto>> getSummary(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) String template,
            @RequestParam(required = false) String cvVariant,
            @RequestParam(required = false) String status) {
        Long currentUserId = securityContextService.getCurrentUserId();
        if (userId != null && !userId.equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for the requested user.");
        }
        LocalDateTime fromDt = toStartOfDay(from);
        LocalDateTime toDt = toEndOfDay(to);
        StatsSummaryDto response = statsService.getSummary(currentUserId, fromDt, toDt, jobTitle, template, cvVariant,
                status);
        return ResponseEntity.ok(ApiResponse.success("Stats summary retrieved successfully", response));
    }

    @GetMapping("/funnel")
    public ResponseEntity<ApiResponse<List<FunnelStageDto>>> getFunnel(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) String template,
            @RequestParam(required = false) String cvVariant,
            @RequestParam(required = false) String status) {
        Long currentUserId = securityContextService.getCurrentUserId();
        if (userId != null && !userId.equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for the requested user.");
        }
        LocalDateTime fromDt = toStartOfDay(from);
        LocalDateTime toDt = toEndOfDay(to);
        List<FunnelStageDto> response = statsService.getFunnel(currentUserId, fromDt, toDt, jobTitle, template,
                cvVariant, status);
        return ResponseEntity.ok(ApiResponse.success("Funnel data retrieved successfully", response));
    }

    @GetMapping("/rejection-stages")
    public ResponseEntity<ApiResponse<List<RejectionStageDto>>> getRejectionStages(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) String template,
            @RequestParam(required = false) String cvVariant,
            @RequestParam(required = false) String status) {
        Long currentUserId = securityContextService.getCurrentUserId();
        if (userId != null && !userId.equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for the requested user.");
        }
        LocalDateTime fromDt = toStartOfDay(from);
        LocalDateTime toDt = toEndOfDay(to);
        List<RejectionStageDto> response = statsService.getRejectionStageBreakdown(currentUserId, fromDt, toDt,
                jobTitle, template, cvVariant, status);
        return ResponseEntity.ok(ApiResponse.success("Rejection stage breakdown retrieved successfully", response));
    }

    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<StatsTrendResponseDto>> getTrends(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "DAY") String granularity,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) String template,
            @RequestParam(required = false) String cvVariant,
            @RequestParam(required = false) String status) {
        Long currentUserId = securityContextService.getCurrentUserId();
        if (userId != null && !userId.equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for the requested user.");
        }
        LocalDateTime fromDt = toStartOfDay(from);
        LocalDateTime toDt = toEndOfDay(to);
        StatsTrendResponseDto response = statsService.getTrendData(currentUserId, fromDt, toDt, granularity, jobTitle,
                template, cvVariant, status);
        return ResponseEntity.ok(ApiResponse.success("Trend data retrieved successfully", response));
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

    // Converts an optional date-only query param into the LocalDateTime bounds
    // the service layer expects. Null in, null out — DateRangeUtils applies the
    // all-time defaults downstream.
    private LocalDateTime toStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    private LocalDateTime toEndOfDay(LocalDate date) {
        return date != null ? date.atTime(23, 59, 59) : null;
    }
}
package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.dtos.FunnelStageDto;
import com.applyflow.tracker_api.dtos.StatsSummaryDto;
import com.applyflow.tracker_api.dtos.TimelineEventDto;
import com.applyflow.tracker_api.models.Application;
import com.applyflow.tracker_api.models.ApplicationEvent;
import com.applyflow.tracker_api.models.ApplicationStatus;
import com.applyflow.tracker_api.repositories.ApplicationEventRepository;
import com.applyflow.tracker_api.repositories.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationEventRepository applicationEventRepository;

    @Transactional(readOnly = true)
    public StatsSummaryDto getSummary(Long userId) {
        long total = applicationRepository.countByUserId(userId);
        long sentCount = countApplicationsThatReached(userId, ApplicationStatus.SENT.name());
        long respondedCount = countApplicationsThatReached(userId, ApplicationStatus.RESPONDED.name());

        double responseRate = sentCount == 0 ? 0.0 : (double) respondedCount / sentCount;
        Double avgResponseDays = computeAvgResponseDays(userId);

        return StatsSummaryDto.builder()
                .totalApplications(total)
                .sentCount(sentCount)
                .responseRate(responseRate)
                .avgResponseDays(avgResponseDays)
                .build();
    }

    @Transactional(readOnly = true)
    public List<FunnelStageDto> getFunnel(Long userId) {
        List<Object[]> rows = applicationEventRepository.countDistinctApplicationsByStatusForUser(userId);
        return rows.stream()
                .map(row -> FunnelStageDto.builder()
                        .status((String) row[0])
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TimelineEventDto> getRecentEvents(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(limit, 50)));
        return applicationEventRepository.findRecentByUser(userId, pageable).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Full history for one application's timeline. Enforces ownership the same
     * way every other ApplicationService method does — 404s (via the thrown
     * exception) if the application doesn't exist or isn't the caller's.
     */
    @Transactional(readOnly = true)
    public List<TimelineEventDto> getApplicationTimeline(Long applicationId, Long userId) {
        Application app = applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new RuntimeException("Application tracking record not found or access denied."));

        return applicationEventRepository.findByApplicationIdOrderByOccurredAtAsc(app.getId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // --- internal helpers ---

    private long countApplicationsThatReached(Long userId, String status) {
        return applicationEventRepository
                .findByApplication_User_IdAndStatusOrderByOccurredAtAsc(userId, status)
                .stream()
                .map(e -> e.getApplication().getId())
                .distinct()
                .count();
    }

    /**
     * Average days between an application's first SENT event and its first
     * RESPONDED event, across every application that has both. Returns null
     * if nothing has been responded to yet, so the frontend can show
     * "not enough data" instead of a misleading 0.
     */
    private Double computeAvgResponseDays(Long userId) {
        Map<Long, LocalDateTime> firstSentByApp = earliestOccurrenceByApplication(userId,
                ApplicationStatus.SENT.name());
        Map<Long, LocalDateTime> firstRespondedByApp = earliestOccurrenceByApplication(userId,
                ApplicationStatus.RESPONDED.name());

        List<Double> daysPerApplication = new ArrayList<>();
        for (Map.Entry<Long, LocalDateTime> entry : firstRespondedByApp.entrySet()) {
            LocalDateTime sentAt = firstSentByApp.get(entry.getKey());
            if (sentAt != null) {
                double days = Duration.between(sentAt, entry.getValue()).toMinutes() / 1440.0;
                daysPerApplication.add(days);
            }
        }

        if (daysPerApplication.isEmpty()) {
            return null;
        }
        return daysPerApplication.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private Map<Long, LocalDateTime> earliestOccurrenceByApplication(Long userId, String status) {
        List<ApplicationEvent> events = applicationEventRepository
                .findByApplication_User_IdAndStatusOrderByOccurredAtAsc(userId, status);

        // events are ordered earliest-first, so putIfAbsent naturally keeps
        // only the first occurrence per application
        Map<Long, LocalDateTime> earliest = new LinkedHashMap<>();
        for (ApplicationEvent event : events) {
            earliest.putIfAbsent(event.getApplication().getId(), event.getOccurredAt());
        }
        return earliest;
    }

    private TimelineEventDto toDto(ApplicationEvent event) {
        return TimelineEventDto.builder()
                .id(event.getId())
                .applicationId(event.getApplication().getId())
                .status(event.getStatus())
                .note(event.getNote())
                .occurredAt(event.getOccurredAt())
                .companyName(event.getApplication().getCompanyName())
                .jobTitle(event.getApplication().getJobTitle())
                .build();
    }
}
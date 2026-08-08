package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.dtos.FunnelStageDto;
import com.applyflow.tracker_api.dtos.RejectionStageDto;
import com.applyflow.tracker_api.dtos.StatsSummaryDto;
import com.applyflow.tracker_api.dtos.TimelineEventDto;
import com.applyflow.tracker_api.models.Application;
import com.applyflow.tracker_api.models.ApplicationEvent;
import com.applyflow.tracker_api.models.ApplicationStatus;
import com.applyflow.tracker_api.repositories.ApplicationEventRepository;
import com.applyflow.tracker_api.repositories.ApplicationRepository;
import com.applyflow.tracker_api.util.DateRangeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private static final List<String> ACTIVE_STATUSES = List.of(
            "SENT", "VIEWED", "RESPONDED", "INTERVIEW_SCHEDULED", "INTERVIEWING");
    private static final List<String> TERMINAL_STATUSES = List.of(
            "OFFER", "REJECTED", "GHOSTED", "WITHDRAWN");

    private final ApplicationRepository applicationRepository;
    private final ApplicationEventRepository applicationEventRepository;

    @Transactional(readOnly = true)
    public StatsSummaryDto getSummary(Long userId, LocalDateTime from, LocalDateTime to) {
        LocalDateTime effFrom = DateRangeUtils.effectiveFrom(from);
        LocalDateTime effTo = DateRangeUtils.effectiveTo(to);

        long total = applicationRepository.countByUserIdInRange(userId, effFrom, effTo);
        long sentCount = countApplicationsThatReached(userId, ApplicationStatus.SENT.name(), effFrom, effTo);
        long respondedCount = countApplicationsThatReached(userId, ApplicationStatus.RESPONDED.name(), effFrom,
                effTo);
        long viewedCount = countApplicationsThatReached(userId, ApplicationStatus.VIEWED.name(), effFrom, effTo);
        long interviewedCount = countApplicationsThatReached(userId, ApplicationStatus.INTERVIEWING.name(), effFrom,
                effTo);
        long offerCount = countApplicationsThatReached(userId, ApplicationStatus.OFFER.name(), effFrom, effTo);

        double responseRate = sentCount == 0 ? 0.0 : (double) respondedCount / sentCount;
        Double avgResponseDays = computeAvgResponseDays(userId, effFrom, effTo);

        long activeCount = applicationRepository.countByUserIdAndStatusIn(userId, ACTIVE_STATUSES);
        long terminalCount = applicationRepository.countByUserIdAndStatusIn(userId, TERMINAL_STATUSES);

        long neverViewedCount = Math.max(sentCount - viewedCount, 0);
        double neverViewedRate = sentCount == 0 ? 0.0 : (double) neverViewedCount / sentCount;

        Double interviewToOfferRate = interviewedCount == 0 ? null : (double) offerCount / interviewedCount;

        return StatsSummaryDto.builder()
                .totalApplications(total)
                .sentCount(sentCount)
                .responseRate(responseRate)
                .avgResponseDays(avgResponseDays)
                .activeCount(activeCount)
                .terminalCount(terminalCount)
                .neverViewedCount(neverViewedCount)
                .neverViewedRate(neverViewedRate)
                .interviewedCount(interviewedCount)
                .offerCount(offerCount)
                .interviewToOfferRate(interviewToOfferRate)
                .build();
    }

    @Transactional(readOnly = true)
    public List<FunnelStageDto> getFunnel(Long userId, LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = applicationEventRepository.countDistinctApplicationsByStatusForUser(
                userId, DateRangeUtils.effectiveFrom(from), DateRangeUtils.effectiveTo(to));
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

    /**
     * Of applications that were rejected, how far did they get first — before
     * an interview was ever scheduled, or after. Walks each rejected
     * application's own timeline rather than a single aggregate query, since
     * "last non-rejected status" isn't expressible cleanly in JPQL. All-time,
     * not date-range scoped.
     */
    @Transactional(readOnly = true)
    public List<RejectionStageDto> getRejectionStageBreakdown(Long userId) {
        List<ApplicationEvent> rejections = applicationEventRepository
                .findByApplication_User_IdAndStatusInRange(
                        userId, ApplicationStatus.REJECTED.name(),
                        DateRangeUtils.effectiveFrom(null), DateRangeUtils.effectiveTo(null));

        Set<String> postInterviewStatuses = Set.of(
                ApplicationStatus.INTERVIEW_SCHEDULED.name(),
                ApplicationStatus.INTERVIEWING.name(),
                ApplicationStatus.OFFER.name());

        long beforeInterview = 0;
        long afterInterview = 0;

        Set<Long> seenApplicationIds = new HashSet<>();
        for (ApplicationEvent rejection : rejections) {
            Long appId = rejection.getApplication().getId();
            if (!seenApplicationIds.add(appId)) {
                continue; // only count each application once
            }
            List<ApplicationEvent> timeline = applicationEventRepository
                    .findByApplicationIdOrderByOccurredAtAsc(appId);

            boolean reachedInterviewStage = timeline.stream()
                    .anyMatch(e -> postInterviewStatuses.contains(e.getStatus()));

            if (reachedInterviewStage) {
                afterInterview++;
            } else {
                beforeInterview++;
            }
        }

        return List.of(
                RejectionStageDto.builder().stage("BEFORE_INTERVIEW").count(beforeInterview).build(),
                RejectionStageDto.builder().stage("AFTER_INTERVIEW").count(afterInterview).build());
    }

    // --- internal helpers ---

    private long countApplicationsThatReached(Long userId, String status, LocalDateTime from, LocalDateTime to) {
        return applicationEventRepository
                .findByApplication_User_IdAndStatusInRange(userId, status, from, to)
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
    private Double computeAvgResponseDays(Long userId, LocalDateTime from, LocalDateTime to) {
        Map<Long, LocalDateTime> firstSentByApp = earliestOccurrenceByApplication(userId,
                ApplicationStatus.SENT.name(), from, to);
        Map<Long, LocalDateTime> firstRespondedByApp = earliestOccurrenceByApplication(userId,
                ApplicationStatus.RESPONDED.name(), from, to);

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

    private Map<Long, LocalDateTime> earliestOccurrenceByApplication(Long userId, String status,
            LocalDateTime from, LocalDateTime to) {
        List<ApplicationEvent> events = applicationEventRepository
                .findByApplication_User_IdAndStatusInRange(userId, status, from, to);

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
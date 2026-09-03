package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.dtos.*;
import com.applyflow.tracker_api.models.Application;
import com.applyflow.tracker_api.models.ApplicationEvent;
import com.applyflow.tracker_api.models.ApplicationStatus;
import com.applyflow.tracker_api.repositories.ApplicationEventRepository;
import com.applyflow.tracker_api.repositories.ApplicationRepository;
import com.applyflow.tracker_api.utils.DateRangeUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

        private static final List<String> ACTIVE_STATUSES = List.of(
                        "SENT", "VIEWED", "RESPONDED", "INTERVIEW_SCHEDULED", "INTERVIEWING");
        private static final List<String> TERMINAL_STATUSES = List.of(
                        "OFFER", "REJECTED", "GHOSTED", "WITHDRAWN");
        private static final List<String> IGNORED_STATUSES = List.of("VIEWED");
        private final ApplicationRepository applicationRepository;
        private final ApplicationEventRepository applicationEventRepository;


        @Transactional(readOnly = true)
        public StatsSummaryDto getSummary(Long userId, LocalDateTime from, LocalDateTime to,
                        String jobTitle, String template, String cvVariant, String status) {
                LocalDateTime effFrom = DateRangeUtils.effectiveFrom(from);
                LocalDateTime effTo = DateRangeUtils.effectiveTo(to);
                StatsPeriodSummaryDto current = buildPeriodSummary(userId, effFrom, effTo, jobTitle, template,
                                cvVariant,
                                status);
                LocalDateTime previousFrom = previousPeriodFrom(from, to);
                LocalDateTime previousTo = previousPeriodTo(from, to);
                StatsPeriodSummaryDto previous = buildPeriodSummary(userId, previousFrom, previousTo, jobTitle,
                                template,
                                cvVariant, status);
                return StatsSummaryDto.fromPeriod(current, previous);
        }

    

        @Transactional(readOnly = true)
        public List<FunnelStageDto> getFunnel(Long userId, LocalDateTime from, LocalDateTime to,
                        String jobTitle, String template, String cvVariant, String status) {
                LocalDateTime effFrom = DateRangeUtils.effectiveFrom(from);
                LocalDateTime effTo = DateRangeUtils.effectiveTo(to);

                boolean hasFilters = jobTitle != null || template != null || cvVariant != null || status != null;
                List<Object[]> rows;
                if (hasFilters) {
                        List<Long> applicationIds = applicationRepository.findApplicationIdsByUserIdAndFilters(
                                        userId, effFrom, effTo, jobTitle, template, cvVariant, status);
                        if (applicationIds.isEmpty()) {
                                return List.of();
                        }
                        rows = applicationEventRepository.countDistinctApplicationsByStatusForUserAndApplicationIds(
                                        userId, applicationIds, effFrom, effTo);
                } else {
                        rows = applicationEventRepository.countDistinctApplicationsByStatusForUser(userId, effFrom,
                                        effTo);
                }

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

        @Transactional(readOnly = true)
        public List<TimelineEventDto> getApplicationTimeline(Long applicationId, Long userId) {
                Application app = applicationRepository.findByIdAndUserId(applicationId, userId)
                                .orElseThrow(() -> new RuntimeException("Application not found or access denied."));

                return applicationEventRepository.findByApplicationIdOrderByOccurredAtAsc(app.getId()).stream()
                                .map(this::toDto)
                                .collect(Collectors.toList());
        }


        @Transactional(readOnly = true)
        public List<RejectionStageDto> getRejectionStageBreakdown(Long userId, LocalDateTime from, LocalDateTime to,
                        String jobTitle, String template, String cvVariant, String status) {
                LocalDateTime effFrom = DateRangeUtils.effectiveFrom(from);
                LocalDateTime effTo = DateRangeUtils.effectiveTo(to);

                List<ApplicationEvent> rejections = applicationEventRepository
                                .findByApplication_User_IdAndStatusInRange(
                                                userId, ApplicationStatus.REJECTED.name(), effFrom, effTo);

                List<Long> filteredAppIds = applicationRepository.findApplicationIdsByUserIdAndFilters(
                                userId, effFrom, effTo, jobTitle, template, cvVariant, status);
                Set<Long> appIdSet = new HashSet<>(filteredAppIds);

                List<ApplicationEvent> scopedRejections = rejections.stream()
                                .filter(event -> appIdSet.isEmpty()
                                                || appIdSet.contains(event.getApplication().getId()))
                                .collect(Collectors.toList());

                Set<String> postInterviewStatuses = Set.of(
                                ApplicationStatus.INTERVIEW_SCHEDULED.name(),
                                ApplicationStatus.INTERVIEWING.name(),
                                ApplicationStatus.OFFER.name());

                List<Long> rejectedAppIds = scopedRejections.stream()
                                .map(event -> event.getApplication().getId())
                                .distinct()
                                .collect(Collectors.toList());

                long beforeInterview = 0;
                long afterInterview = 0;

                if (!rejectedAppIds.isEmpty()) {
                        // Single batched fetch instead of one query per rejected application.
                        Map<Long, List<ApplicationEvent>> timelinesByApp = applicationEventRepository
                                        .findByApplicationIdInOrderByApplicationIdAscOccurredAtAsc(rejectedAppIds)
                                        .stream()
                                        .collect(Collectors.groupingBy(e -> e.getApplication().getId()));

                        for (Long appId : rejectedAppIds) {
                                List<ApplicationEvent> timeline = timelinesByApp.getOrDefault(appId, List.of());
                                boolean reachedInterviewStage = timeline.stream()
                                                .anyMatch(e -> postInterviewStatuses.contains(e.getStatus()));
                                if (reachedInterviewStage) {
                                        afterInterview++;
                                } else {
                                        beforeInterview++;
                                }
                        }
                }

                return List.of(
                                RejectionStageDto.builder().stage("BEFORE_INTERVIEW").count(beforeInterview).build(),
                                RejectionStageDto.builder().stage("AFTER_INTERVIEW").count(afterInterview).build());
        }


     
   
        // ---- Summary (unchanged path — single current + single previous call) --

        private StatsPeriodSummaryDto buildPeriodSummary(Long userId, LocalDateTime from, LocalDateTime to,
                        String jobTitle, String template, String cvVariant, String status) {
                LocalDateTime effFrom = DateRangeUtils.effectiveFrom(from);
                LocalDateTime effTo = DateRangeUtils.effectiveTo(to);
                List<Long> applicationIds = applicationRepository.findApplicationIdsByUserIdAndFilters(userId, effFrom,
                                effTo,
                                jobTitle, template, cvVariant, status);
                long total = applicationIds.size();
                long sentCount = countApplicationsThatReached(userId, ApplicationStatus.SENT.name(), effFrom, effTo,
                                applicationIds, jobTitle, template, cvVariant, status);
                long respondedCount = countApplicationsThatReached(userId, ApplicationStatus.RESPONDED.name(), effFrom,
                                effTo,
                                applicationIds, jobTitle, template, cvVariant, status);
                long viewedCount = countApplicationsThatReached(userId, ApplicationStatus.VIEWED.name(), effFrom, effTo,
                                applicationIds, jobTitle, template, cvVariant, status);
                long interviewedCount = countApplicationsThatReached(userId, ApplicationStatus.INTERVIEWING.name(),
                                effFrom,
                                effTo, applicationIds, jobTitle, template, cvVariant, status);
                long offerCount = countApplicationsThatReached(userId, ApplicationStatus.OFFER.name(), effFrom, effTo,
                                applicationIds, jobTitle, template, cvVariant, status);
                long activeCount = applicationRepository.countByUserIdAndStatusInFilters(userId, ACTIVE_STATUSES,
                                effFrom,
                                effTo, jobTitle, template, cvVariant, status);
                long terminalCount = applicationRepository.countByUserIdAndStatusInFilters(userId, TERMINAL_STATUSES,
                                effFrom,
                                effTo, jobTitle, template, cvVariant, status);
                long ignoredCount = applicationRepository.countByUserIdAndStatusInFilters(userId, IGNORED_STATUSES,
                                effFrom,
                                effTo, jobTitle, template, cvVariant, status);
                long neverViewedCount = Math.max(sentCount - viewedCount, 0);
                double responseRate = sentCount == 0 ? 0.0 : (double) respondedCount / sentCount;
                double neverViewedRate = sentCount == 0 ? 0.0 : (double) neverViewedCount / sentCount;
                double ignoredRate = viewedCount == 0 ? 0.0 : (double) ignoredCount / viewedCount;
                Double interviewToOfferRate = interviewedCount == 0 ? null : (double) offerCount / interviewedCount;
                Double avgResponseDays = computeAvgResponseDays(userId, effFrom, effTo, applicationIds, jobTitle,
                                template,
                                cvVariant, status);

                return StatsPeriodSummaryDto.builder()
                                .totalApplications(total)
                                .sentCount(sentCount)
                                .respondedCount(respondedCount)
                                .viewedCount(viewedCount)
                                .responseRate(responseRate)
                                .avgResponseDays(avgResponseDays)
                                .activeCount(activeCount)
                                .terminalCount(terminalCount)
                                .neverViewedCount(neverViewedCount)
                                .neverViewedRate(neverViewedRate)
                                .ignoredCount(ignoredCount)
                                .ignoredRate(ignoredRate)
                                .interviewedCount(interviewedCount)
                                .offerCount(offerCount)
                                .interviewToOfferRate(interviewToOfferRate)
                                .build();
        }

        private long countApplicationsThatReached(Long userId, String status, LocalDateTime from, LocalDateTime to,
                        List<Long> applicationIds, String jobTitle, String template, String cvVariant,
                        String filterStatus) {
                if (applicationIds == null || applicationIds.isEmpty()) {
                        return 0L;
                }
                return applicationEventRepository.countDistinctApplicationsByStatusAndApplicationIds(
                                userId, status, applicationIds, from, to);
        }

        private Double computeAvgResponseDays(Long userId, LocalDateTime from, LocalDateTime to,
                        List<Long> applicationIds, String jobTitle, String template, String cvVariant, String status) {
                if (applicationIds == null || applicationIds.isEmpty()) {
                        return null;
                }
                Map<Long, LocalDateTime> firstSentByApp = new LinkedHashMap<>();
                for (ApplicationEvent event : applicationEventRepository.findEarliestEventByApplicationIds(userId,
                                ApplicationStatus.SENT.name(), applicationIds, from, to)) {
                        firstSentByApp.putIfAbsent(event.getApplication().getId(), event.getOccurredAt());
                }
                Map<Long, LocalDateTime> firstRespondedByApp = new LinkedHashMap<>();
                for (ApplicationEvent event : applicationEventRepository.findEarliestEventByApplicationIds(userId,
                                ApplicationStatus.RESPONDED.name(), applicationIds, from, to)) {
                        firstRespondedByApp.putIfAbsent(event.getApplication().getId(), event.getOccurredAt());
                }

                List<Double> daysPerApplication = new ArrayList<>();
                for (Map.Entry<Long, LocalDateTime> entry : firstRespondedByApp.entrySet()) {
                        LocalDateTime sentAt = firstSentByApp.get(entry.getKey());
                        if (sentAt != null) {
                                daysPerApplication.add((double) Duration.between(sentAt, entry.getValue()).toMinutes()
                                                / 1440.0);
                        }
                }
                if (daysPerApplication.isEmpty()) {
                        return null;
                }
                return daysPerApplication.stream()
                                .filter(Objects::nonNull)
                                .mapToDouble(value -> value.doubleValue())
                                .average()
                                .orElse(0.0);
        }

        
        private LocalDateTime previousPeriodFrom(LocalDateTime from, LocalDateTime to) {
                if (from == null || to == null) {
                        return DateRangeUtils.effectiveFrom(null);
                }
                long days = ChronoUnit.DAYS.between(from.toLocalDate(), to.toLocalDate()) + 1;
                return from.minusDays(days);
        }

      
        private LocalDateTime previousPeriodTo(LocalDateTime from, LocalDateTime to) {
                if (from == null || to == null) {
                        return DateRangeUtils.effectiveTo(null);
                }
                return from.minusNanos(1_000_000L);
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
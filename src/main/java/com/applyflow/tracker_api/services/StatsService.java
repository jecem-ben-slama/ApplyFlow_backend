package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.dtos.*;
import com.applyflow.tracker_api.models.Application;
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
        private static final List<String> SUMMARY_EVENT_STATUSES = List.of(
                        ApplicationStatus.SENT.name(),
                        ApplicationStatus.RESPONDED.name(),
                        ApplicationStatus.VIEWED.name(),
                        ApplicationStatus.INTERVIEWING.name(),
                        ApplicationStatus.OFFER.name());
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
                return applicationEventRepository.findRecentDtosByUser(userId, pageable);
        }

        @Transactional(readOnly = true)
        public List<TimelineEventDto> getApplicationTimeline(Long applicationId, Long userId) {
                Application app = applicationRepository.findByIdAndUserId(applicationId, userId)
                                .orElseThrow(() -> new RuntimeException("Application not found or access denied."));
                return applicationEventRepository.findTimelineDtosByApplicationIdAndUserId(app.getId(), userId);
        }

        @Transactional(readOnly = true)
        public List<RejectionStageDto> getRejectionStageBreakdown(Long userId, LocalDateTime from, LocalDateTime to,
                        String jobTitle, String template, String cvVariant, String status) {
                LocalDateTime effFrom = DateRangeUtils.effectiveFrom(from);
                LocalDateTime effTo = DateRangeUtils.effectiveTo(to);

                boolean hasFilters = jobTitle != null || template != null || cvVariant != null || status != null;
                List<Long> rejectedAppIds = applicationEventRepository.findDistinctApplicationIdsByUserAndStatusInRange(
                                userId, ApplicationStatus.REJECTED.name(), effFrom, effTo);
                List<Long> filteredAppIds = applicationRepository.findApplicationIdsByUserIdAndFilters(
                                userId, effFrom, effTo, jobTitle, template, cvVariant, status);
                if (hasFilters) {
                        rejectedAppIds = rejectedAppIds.stream()
                                        .filter(new HashSet<>(filteredAppIds)::contains)
                                        .collect(Collectors.toList());
                }

                Set<String> postInterviewStatuses = Set.of(
                                ApplicationStatus.INTERVIEW_SCHEDULED.name(),
                                ApplicationStatus.INTERVIEWING.name(),
                                ApplicationStatus.OFFER.name());

                long beforeInterview = 0;
                long afterInterview = 0;

                if (!rejectedAppIds.isEmpty()) {
                        Set<Long> afterInterviewAppIds = new HashSet<>(applicationEventRepository
                                        .findApplicationIdsWithStatuses(rejectedAppIds,
                                                        new ArrayList<>(postInterviewStatuses)));
                        afterInterview = afterInterviewAppIds.size();
                        beforeInterview = rejectedAppIds.size() - afterInterview;
                }

                return List.of(
                                RejectionStageDto.builder().stage("BEFORE_INTERVIEW").count(beforeInterview).build(),
                                RejectionStageDto.builder().stage("AFTER_INTERVIEW").count(afterInterview).build());
        }

        // ---- Summary (unchanged path — single current + single previous call) --

        private StatsPeriodSummaryDto buildPeriodSummary(Long userId, LocalDateTime from, LocalDateTime to,
                        String jobTitle, String template, String cvVariant, String status) {
                List<Long> applicationIds = applicationRepository.findApplicationIdsByUserIdAndFilters(userId, from,
                                to,
                                jobTitle, template, cvVariant, status);
                long total = applicationIds.size();
                Map<String, Long> reachedCounts = countReachedByStatus(userId, applicationIds, from, to);
                long sentCount = reachedCounts.getOrDefault(ApplicationStatus.SENT.name(), 0L);
                long respondedCount = reachedCounts.getOrDefault(ApplicationStatus.RESPONDED.name(), 0L);
                long viewedCount = reachedCounts.getOrDefault(ApplicationStatus.VIEWED.name(), 0L);
                long interviewedCount = reachedCounts.getOrDefault(ApplicationStatus.INTERVIEWING.name(), 0L);
                long offerCount = reachedCounts.getOrDefault(ApplicationStatus.OFFER.name(), 0L);

                Map<String, Long> currentStatusCounts = countCurrentStatusesByStatus(userId, applicationIds, from, to,
                                jobTitle, template, cvVariant, status);
                long activeCount = countStatuses(currentStatusCounts, ACTIVE_STATUSES);
                long terminalCount = countStatuses(currentStatusCounts, TERMINAL_STATUSES);
                long ignoredCount = countStatuses(currentStatusCounts, IGNORED_STATUSES);
                long neverViewedCount = Math.max(sentCount - viewedCount, 0);
                double responseRate = sentCount == 0 ? 0.0 : (double) respondedCount / sentCount;
                double neverViewedRate = sentCount == 0 ? 0.0 : (double) neverViewedCount / sentCount;
                double ignoredRate = viewedCount == 0 ? 0.0 : (double) ignoredCount / viewedCount;
                Double interviewToOfferRate = interviewedCount == 0 ? null : (double) offerCount / interviewedCount;
                Double avgResponseDays = computeAvgResponseDays(userId, from, to, applicationIds);

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

        private Map<String, Long> countReachedByStatus(Long userId, List<Long> applicationIds,
                        LocalDateTime from, LocalDateTime to) {
                if (applicationIds.isEmpty()) {
                        return Map.of();
                }
                return toCountMap(applicationEventRepository.countDistinctApplicationsByStatusesAndApplicationIds(
                                userId, SUMMARY_EVENT_STATUSES, applicationIds, from, to));
        }

        private Map<String, Long> countCurrentStatusesByStatus(Long userId, List<Long> applicationIds,
                        LocalDateTime from, LocalDateTime to, String jobTitle, String template, String cvVariant,
                        String status) {
                if (applicationIds.isEmpty()) {
                        return Map.of();
                }
                List<String> statuses = new ArrayList<>();
                statuses.addAll(ACTIVE_STATUSES);
                statuses.addAll(TERMINAL_STATUSES);
                statuses.addAll(IGNORED_STATUSES);
                return toCountMap(applicationRepository.countByUserIdAndStatusesInFilters(userId, statuses, from, to,
                                jobTitle, template, cvVariant, status));
        }

        private Map<String, Long> toCountMap(List<StatsStatusCountDto> counts) {
                Map<String, Long> result = new HashMap<>();
                for (StatsStatusCountDto count : counts) {
                        result.put(count.status(), count.count());
                }
                return result;
        }

        private long countStatuses(Map<String, Long> counts, List<String> statuses) {
                return statuses.stream().mapToLong(status -> counts.getOrDefault(status, 0L)).sum();
        }

        private Double computeAvgResponseDays(Long userId, LocalDateTime from, LocalDateTime to,
                        List<Long> applicationIds) {
                if (applicationIds.isEmpty()) {
                        return null;
                }
                Map<Long, LocalDateTime> firstSentByApp = new LinkedHashMap<>();
                Map<Long, LocalDateTime> firstRespondedByApp = new LinkedHashMap<>();
                for (StatsEventTimeDto event : applicationEventRepository.findEarliestEventsByApplicationIdsAndStatuses(
                                userId, List.of(ApplicationStatus.SENT.name(), ApplicationStatus.RESPONDED.name()),
                                applicationIds, from, to)) {
                        if (ApplicationStatus.SENT.name().equals(event.status())) {
                                firstSentByApp.put(event.applicationId(), event.occurredAt());
                        } else if (ApplicationStatus.RESPONDED.name().equals(event.status())) {
                                firstRespondedByApp.put(event.applicationId(), event.occurredAt());
                        }
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

}
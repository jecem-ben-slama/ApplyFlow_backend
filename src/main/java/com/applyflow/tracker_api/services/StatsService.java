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
import java.time.LocalDate;
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
        private static final Set<String> ACTIVE_STATUS_SET = new HashSet<>(ACTIVE_STATUSES);
        private static final Set<String> TERMINAL_STATUS_SET = new HashSet<>(TERMINAL_STATUSES);
        private static final List<String> TREND_TRACKED_STATUSES = List.of(
                        ApplicationStatus.SENT.name(), ApplicationStatus.RESPONDED.name(),
                        ApplicationStatus.VIEWED.name(), ApplicationStatus.INTERVIEWING.name(),
                        ApplicationStatus.OFFER.name());

        private final ApplicationRepository applicationRepository;
        private final ApplicationEventRepository applicationEventRepository;

        @Transactional(readOnly = true)
        public StatsSummaryDto getSummary(Long userId, LocalDateTime from, LocalDateTime to) {
                return getSummary(userId, from, to, null, null, null, null);
        }

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
        public List<FunnelStageDto> getFunnel(Long userId, LocalDateTime from, LocalDateTime to) {
                return getFunnel(userId, from, to, null, null, null, null);
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
        public List<RejectionStageDto> getRejectionStageBreakdown(Long userId) {
                return getRejectionStageBreakdown(userId, null, null, null, null, null, null);
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

        // ---- Trend data --------------------------------------------------------
        // Previously this called buildPeriodSummary (≈7 queries) once per day per
        // bucket (current + previous), i.e. ~14 * N queries for an N-day range —
        // 400+ round trips for a 30-day chart. It now fetches every application
        // and every relevant event for the whole range in exactly 2 queries, then
        // buckets everything by calendar day in memory.

        @Transactional(readOnly = true)
        public StatsTrendResponseDto getTrendData(Long userId, LocalDateTime from, LocalDateTime to,
                        String granularity, String jobTitle, String template, String cvVariant, String status) {
                LocalDateTime effFrom = DateRangeUtils.effectiveFrom(from);
                LocalDateTime effTo = DateRangeUtils.effectiveTo(to);
                List<LocalDate> dayPoints = buildDatePoints(effFrom, effTo, granularity);

                // Extend the fetch window one day earlier than the visible range so the
                // very first visible day still has a real "day before" to diff against —
                // matches the original bucket-vs-bucket-minus-1-day comparison.
                LocalDateTime fetchFrom = effFrom.toLocalDate().minusDays(1).atStartOfDay();
                LocalDateTime fetchTo = effTo;

                List<ApplicationBucketDto> buckets = applicationRepository.findApplicationBucketDataByUserIdAndFilters(
                                userId, fetchFrom, fetchTo, jobTitle, template, cvVariant, status);

                Map<LocalDate, List<ApplicationBucketDto>> bucketsByDay = buckets.stream()
                                .filter(b -> b.getDateApplied() != null)
                                .collect(Collectors.groupingBy(b -> b.getDateApplied().toLocalDate()));

                List<Long> appIds = buckets.stream()
                                .map(ApplicationBucketDto::getId)
                                .distinct()
                                .collect(Collectors.toList());

                List<ApplicationEvent> events = appIds.isEmpty()
                                ? List.of()
                                : applicationEventRepository.findByApplicationIdInAndStatusInAndOccurredAtBetween(
                                                appIds, TREND_TRACKED_STATUSES, fetchFrom, fetchTo);

                // day -> status -> distinct application ids that reached that status that day
                Map<LocalDate, Map<String, Set<Long>>> eventsByDayAndStatus = new HashMap<>();
                for (ApplicationEvent event : events) {
                        LocalDate day = event.getOccurredAt().toLocalDate();
                        eventsByDayAndStatus
                                        .computeIfAbsent(day, d -> new HashMap<>())
                                        .computeIfAbsent(event.getStatus(), s -> new HashSet<>())
                                        .add(event.getApplication().getId());
                }

                Set<LocalDate> daysToCompute = new HashSet<>(bucketsByDay.keySet());
                daysToCompute.addAll(eventsByDayAndStatus.keySet());
                Map<LocalDate, PeriodAgg> aggByDay = new HashMap<>();
                for (LocalDate day : daysToCompute) {
                        aggByDay.put(day, computeDayAgg(day, bucketsByDay, eventsByDayAndStatus));
                }

                List<TrendPointDto> applicationsOverTime = new ArrayList<>();
                List<TrendPointDto> responseRateOverTime = new ArrayList<>();
                List<TrendPointDto> interviewToOfferRateOverTime = new ArrayList<>();
                List<TrendPointDto> rejectionTrendOverTime = new ArrayList<>();

                PeriodAgg empty = new PeriodAgg();
                for (LocalDate date : dayPoints) {
                        PeriodAgg currentAgg = aggByDay.getOrDefault(date, empty);
                        PeriodAgg previousAgg = aggByDay.getOrDefault(date.minusDays(1), empty);

                        applicationsOverTime.add(TrendPointDto.builder().date(date).value(currentAgg.totalApplications)
                                        .currentValue(currentAgg.totalApplications)
                                        .previousValue(previousAgg.totalApplications).build());
                        responseRateOverTime.add(TrendPointDto.builder().date(date).percent(currentAgg.responseRate())
                                        .currentValue((long) currentAgg.responseRate())
                                        .previousValue((long) previousAgg.responseRate()).build());
                        interviewToOfferRateOverTime.add(TrendPointDto.builder().date(date)
                                        .percent(currentAgg.interviewToOfferRate() == null ? 0.0
                                                        : currentAgg.interviewToOfferRate())
                                        .currentValue(currentAgg.offerCount).previousValue(previousAgg.offerCount)
                                        .build());
                        rejectionTrendOverTime.add(TrendPointDto.builder().date(date).value(currentAgg.terminalCount)
                                        .currentValue(currentAgg.terminalCount).previousValue(previousAgg.terminalCount)
                                        .build());
                }

                return StatsTrendResponseDto.builder()
                                .granularity(granularity)
                                .applicationsOverTime(applicationsOverTime)
                                .responseRateOverTime(responseRateOverTime)
                                .interviewToOfferRateOverTime(interviewToOfferRateOverTime)
                                .rejectionTrendOverTime(rejectionTrendOverTime)
                                .build();
        }

        private PeriodAgg computeDayAgg(LocalDate day, Map<LocalDate, List<ApplicationBucketDto>> bucketsByDay,
                        Map<LocalDate, Map<String, Set<Long>>> eventsByDayAndStatus) {
                PeriodAgg agg = new PeriodAgg();
                List<ApplicationBucketDto> dayBuckets = bucketsByDay.getOrDefault(day, List.of());
                agg.totalApplications = dayBuckets.size();
                for (ApplicationBucketDto bucket : dayBuckets) {
                        String currentStatus = bucket.getStatus();
                        if (currentStatus != null && ACTIVE_STATUS_SET.contains(currentStatus)) {
                                agg.activeCount++;
                        }
                        if (currentStatus != null && TERMINAL_STATUS_SET.contains(currentStatus)) {
                                agg.terminalCount++;
                        }
                }

                Map<String, Set<Long>> dayEvents = eventsByDayAndStatus.getOrDefault(day, Map.of());
                agg.sentCount = dayEvents.getOrDefault(ApplicationStatus.SENT.name(), Set.of()).size();
                agg.respondedCount = dayEvents.getOrDefault(ApplicationStatus.RESPONDED.name(), Set.of()).size();
                agg.viewedCount = dayEvents.getOrDefault(ApplicationStatus.VIEWED.name(), Set.of()).size();
                agg.interviewedCount = dayEvents.getOrDefault(ApplicationStatus.INTERVIEWING.name(), Set.of()).size();
                agg.offerCount = dayEvents.getOrDefault(ApplicationStatus.OFFER.name(), Set.of()).size();

                return agg;
        }

        // Per-day aggregate used only inside getTrendData's in-memory bucketing.
        private static class PeriodAgg {
                long totalApplications;
                long sentCount;
                long respondedCount;
                long viewedCount;
                long interviewedCount;
                long offerCount;
                long activeCount;
                long terminalCount;

                double responseRate() {
                        return sentCount == 0 ? 0.0 : (double) respondedCount / sentCount;
                }

                Double interviewToOfferRate() {
                        return interviewedCount == 0 ? null : (double) offerCount / interviewedCount;
                }
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
                long neverViewedCount = Math.max(sentCount - viewedCount, 0);
                double responseRate = sentCount == 0 ? 0.0 : (double) respondedCount / sentCount;
                double neverViewedRate = sentCount == 0 ? 0.0 : (double) neverViewedCount / sentCount;
                Double interviewToOfferRate = interviewedCount == 0 ? null : (double) offerCount / interviewedCount;
                Double avgResponseDays = computeAvgResponseDays(userId, effFrom, effTo, applicationIds, jobTitle,
                                template,
                                cvVariant, status);

                return StatsPeriodSummaryDto.builder()
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

        // NOTE: takes the RAW (possibly null) from/to, not the effective (defaulted)
        // bounds. If the caller didn't supply an explicit range — i.e. this is an
        // "all time" request — there is no meaningful "previous period" to compute,
        // so we just reuse the same all-time bounds. Previously this received the
        // already-defaulted 1970–9999 range and tried to shift it further into the
        // past, producing a multi-thousand-year-BC timestamp that Postgres rejected.
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
                long days = ChronoUnit.DAYS.between(from.toLocalDate(), to.toLocalDate()) + 1;
                return from.minusNanos(1_000_000L).minusDays(days - 1);
        }

        private List<LocalDate> buildDatePoints(LocalDateTime from, LocalDateTime to, String granularity) {
                List<LocalDate> dates = new ArrayList<>();
                LocalDate start = from.toLocalDate();
                LocalDate end = to.toLocalDate();
                for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                        dates.add(date);
                }
                return dates;
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
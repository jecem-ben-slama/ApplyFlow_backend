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
        StatsPeriodSummaryDto current = buildPeriodSummary(userId, effFrom, effTo, jobTitle, template, cvVariant,
                status);
        LocalDateTime previousFrom = previousPeriodFrom(from, to);
        LocalDateTime previousTo = previousPeriodTo(from, to);
        StatsPeriodSummaryDto previous = buildPeriodSummary(userId, previousFrom, previousTo, jobTitle, template,
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
        List<Object[]> rows = applicationEventRepository.countDistinctApplicationsByStatusForUser(
                userId, effFrom, effTo);
        List<FunnelStageDto> result = rows.stream()
                .map(row -> FunnelStageDto.builder()
                        .status((String) row[0])
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());

        if (result.isEmpty()) {
            return List.of();
        }
        return result;
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
                .orElseThrow(() -> new RuntimeException("Application tracking record not found or access denied."));

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
                .filter(event -> appIdSet.isEmpty() || appIdSet.contains(event.getApplication().getId()))
                .collect(Collectors.toList());

        Set<String> postInterviewStatuses = Set.of(
                ApplicationStatus.INTERVIEW_SCHEDULED.name(),
                ApplicationStatus.INTERVIEWING.name(),
                ApplicationStatus.OFFER.name());

        long beforeInterview = 0;
        long afterInterview = 0;
        Set<Long> seenApplicationIds = new HashSet<>();
        for (ApplicationEvent rejection : scopedRejections) {
            Long appId = rejection.getApplication().getId();
            if (!seenApplicationIds.add(appId)) {
                continue;
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

    @Transactional(readOnly = true)
    public StatsTrendResponseDto getTrendData(Long userId, LocalDateTime from, LocalDateTime to,
            String granularity, String jobTitle, String template, String cvVariant, String status) {
        LocalDateTime effFrom = DateRangeUtils.effectiveFrom(from);
        LocalDateTime effTo = DateRangeUtils.effectiveTo(to);
        List<LocalDate> dayPoints = buildDatePoints(effFrom, effTo, granularity);
        List<TrendPointDto> applicationsOverTime = new ArrayList<>();
        List<TrendPointDto> responseRateOverTime = new ArrayList<>();
        List<TrendPointDto> interviewToOfferRateOverTime = new ArrayList<>();
        List<TrendPointDto> rejectionTrendOverTime = new ArrayList<>();

        for (LocalDate date : dayPoints) {
            LocalDateTime bucketStart = date.atStartOfDay();
            LocalDateTime bucketEnd = date.plusDays(1).atStartOfDay().minusNanos(1_000_000L);
            LocalDateTime previousBucketStart = bucketStart.minusDays(1);
            LocalDateTime previousBucketEnd = previousBucketStart.withHour(23).withMinute(59).withSecond(59);
            StatsPeriodSummaryDto currentBucket = buildPeriodSummary(userId, bucketStart, bucketEnd, jobTitle,
                    template, cvVariant, status);
            StatsPeriodSummaryDto previousBucket1 = buildPeriodSummary(userId,
                    previousBucketStart.withHour(0).withMinute(0).withSecond(0),
                    previousBucketEnd, jobTitle, template, cvVariant, status);

            applicationsOverTime.add(TrendPointDto.builder().date(date).value(currentBucket.getTotalApplications())
                    .currentValue(currentBucket.getTotalApplications())
                    .previousValue(previousBucket1.getTotalApplications()).build());
            responseRateOverTime.add(TrendPointDto.builder().date(date).percent(currentBucket.getResponseRate())
                    .currentValue((long) currentBucket.getResponseRate())
                    .previousValue((long) previousBucket1.getResponseRate()).build());
            interviewToOfferRateOverTime.add(TrendPointDto.builder().date(date)
                    .percent(currentBucket.getInterviewToOfferRate() == null ? 0.0
                            : currentBucket.getInterviewToOfferRate())
                    .currentValue(currentBucket.getOfferCount()).previousValue(previousBucket1.getOfferCount())
                    .build());
            rejectionTrendOverTime.add(TrendPointDto.builder().date(date).value(currentBucket.getTerminalCount())
                    .currentValue(currentBucket.getTerminalCount()).previousValue(previousBucket1.getTerminalCount())
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

    private StatsPeriodSummaryDto buildPeriodSummary(Long userId, LocalDateTime from, LocalDateTime to,
            String jobTitle, String template, String cvVariant, String status) {
        LocalDateTime effFrom = DateRangeUtils.effectiveFrom(from);
        LocalDateTime effTo = DateRangeUtils.effectiveTo(to);
        List<Long> applicationIds = applicationRepository.findApplicationIdsByUserIdAndFilters(userId, effFrom, effTo,
                jobTitle, template, cvVariant, status);
        long total = applicationIds.size();
        long sentCount = countApplicationsThatReached(userId, ApplicationStatus.SENT.name(), effFrom, effTo,
                applicationIds, jobTitle, template, cvVariant, status);
        long respondedCount = countApplicationsThatReached(userId, ApplicationStatus.RESPONDED.name(), effFrom, effTo,
                applicationIds, jobTitle, template, cvVariant, status);
        long viewedCount = countApplicationsThatReached(userId, ApplicationStatus.VIEWED.name(), effFrom, effTo,
                applicationIds, jobTitle, template, cvVariant, status);
        long interviewedCount = countApplicationsThatReached(userId, ApplicationStatus.INTERVIEWING.name(), effFrom,
                effTo, applicationIds, jobTitle, template, cvVariant, status);
        long offerCount = countApplicationsThatReached(userId, ApplicationStatus.OFFER.name(), effFrom, effTo,
                applicationIds, jobTitle, template, cvVariant, status);
        long activeCount = applicationRepository.countByUserIdAndStatusInFilters(userId, ACTIVE_STATUSES, effFrom,
                effTo, jobTitle, template, cvVariant, status);
        long terminalCount = applicationRepository.countByUserIdAndStatusInFilters(userId, TERMINAL_STATUSES, effFrom,
                effTo, jobTitle, template, cvVariant, status);
        long neverViewedCount = Math.max(sentCount - viewedCount, 0);
        double responseRate = sentCount == 0 ? 0.0 : (double) respondedCount / sentCount;
        double neverViewedRate = sentCount == 0 ? 0.0 : (double) neverViewedCount / sentCount;
        Double interviewToOfferRate = interviewedCount == 0 ? null : (double) offerCount / interviewedCount;
        Double avgResponseDays = computeAvgResponseDays(userId, effFrom, effTo, applicationIds, jobTitle, template,
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
            List<Long> applicationIds, String jobTitle, String template, String cvVariant, String filterStatus) {
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
                daysPerApplication.add((double) Duration.between(sentAt, entry.getValue()).toMinutes() / 1440.0);
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
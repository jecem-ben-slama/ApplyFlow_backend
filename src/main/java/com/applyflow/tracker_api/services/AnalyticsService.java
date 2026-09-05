package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.dtos.ApplicationSummaryDto;
import com.applyflow.tracker_api.dtos.StatMetricDto;
import com.applyflow.tracker_api.models.ApplicationStatus;
import com.applyflow.tracker_api.repositories.AnalyticsRepository;
import com.applyflow.tracker_api.repositories.ApplicationRepository;
import com.applyflow.tracker_api.utils.DateRangeUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final List<String> DEFAULT_SUCCESS_STATUSES = List.of("INTERVIEWING", "OFFER");

    private final AnalyticsRepository analyticsRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public List<StatMetricDto> getCvVariantStats(Long userId, List<String> successStatuses,
            LocalDateTime from, LocalDateTime to) {
        AnalyticsRange range = resolveRange(from, to);
        return calculateRates(analyticsRepository.getStatsByCvVariant(
                userId, resolveSuccessStatuses(successStatuses), range.from(), range.to()));
    }

    @Transactional(readOnly = true)
    public List<StatMetricDto> getLanguageStats(Long userId, List<String> successStatuses,
            LocalDateTime from, LocalDateTime to) {
        AnalyticsRange range = resolveRange(from, to);
        return calculateRates(analyticsRepository.getStatsByLanguage(
                userId, resolveSuccessStatuses(successStatuses), range.from(), range.to()));
    }

    @Transactional(readOnly = true)
    public List<StatMetricDto> getJobTitleStats(Long userId, List<String> successStatuses,
            LocalDateTime from, LocalDateTime to) {
        AnalyticsRange range = resolveRange(from, to);
        return calculateRates(analyticsRepository.getStatsByJobTitle(
                userId, resolveSuccessStatuses(successStatuses), range.from(), range.to()));
    }

    @Transactional(readOnly = true)
    public List<StatMetricDto> getTemplateStats(Long userId, List<String> successStatuses,
            LocalDateTime from, LocalDateTime to) {
        AnalyticsRange range = resolveRange(from, to);
        return calculateRates(analyticsRepository.getStatsByTemplate(
                userId, resolveSuccessStatuses(successStatuses), range.from(), range.to()));
    }

    @Transactional(readOnly = true)
    public List<ApplicationSummaryDto> listApplicationSummaries(Long userId) {
        return applicationRepository.findSummariesByUserId(userId);
    }

    private List<String> resolveSuccessStatuses(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return DEFAULT_SUCCESS_STATUSES;
        }

        return requested.stream()
                .map(status -> status == null ? "" : status.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .peek(status -> {
                    if (!ApplicationStatus.isValid(status)) {
                        throw new IllegalArgumentException("Invalid application status: " + status);
                    }
                })
                .toList();
    }

    private AnalyticsRange resolveRange(LocalDateTime from, LocalDateTime to) {
        return new AnalyticsRange(
                DateRangeUtils.effectiveFrom(from),
                DateRangeUtils.effectiveTo(to));
    }

    private record AnalyticsRange(LocalDateTime from, LocalDateTime to) {
    }

    private List<StatMetricDto> calculateRates(List<StatMetricDto> stats) {
        for (StatMetricDto stat : stats) {
            if (stat.getTotalApplications() > 0) {
                double rate = ((double) stat.getSuccessCount() / stat.getTotalApplications()) * 100.0;
                stat.setSuccessRatePercentage(Math.round(rate * 100.0) / 100.0);
            } else {
                stat.setSuccessRatePercentage(0.0);
            }
        }
        return stats;
    }
}
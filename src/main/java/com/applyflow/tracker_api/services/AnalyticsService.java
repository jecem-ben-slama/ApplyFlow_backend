package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.dtos.ApplicationSummaryDto;
import com.applyflow.tracker_api.dtos.StatMetricDto;
import com.applyflow.tracker_api.repositories.AnalyticsRepository;
import com.applyflow.tracker_api.repositories.ApplicationRepository;
import com.applyflow.tracker_api.util.DateRangeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final List<String> DEFAULT_SUCCESS_STATUSES = List.of("INTERVIEWING", "OFFER");

    private final AnalyticsRepository analyticsRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public List<StatMetricDto> getCvVariantStats(Long userId, List<String> successStatuses,
            LocalDateTime from, LocalDateTime to) {
        return calculateRates(analyticsRepository.getStatsByCvVariant(
                userId, resolveSuccessStatuses(successStatuses),
                DateRangeUtils.effectiveFrom(from), DateRangeUtils.effectiveTo(to)));
    }

    @Transactional(readOnly = true)
    public List<StatMetricDto> getLanguageStats(Long userId, List<String> successStatuses,
            LocalDateTime from, LocalDateTime to) {
        return calculateRates(analyticsRepository.getStatsByLanguage(
                userId, resolveSuccessStatuses(successStatuses),
                DateRangeUtils.effectiveFrom(from), DateRangeUtils.effectiveTo(to)));
    }

    @Transactional(readOnly = true)
    public List<StatMetricDto> getJobTitleStats(Long userId, List<String> successStatuses,
            LocalDateTime from, LocalDateTime to) {
        return calculateRates(analyticsRepository.getStatsByJobTitle(
                userId, resolveSuccessStatuses(successStatuses),
                DateRangeUtils.effectiveFrom(from), DateRangeUtils.effectiveTo(to)));
    }

    @Transactional(readOnly = true)
    public List<StatMetricDto> getTemplateStats(Long userId, List<String> successStatuses,
            LocalDateTime from, LocalDateTime to) {
        return calculateRates(analyticsRepository.getStatsByTemplate(
                userId, resolveSuccessStatuses(successStatuses),
                DateRangeUtils.effectiveFrom(from), DateRangeUtils.effectiveTo(to)));
    }

    @Transactional(readOnly = true)
    public List<ApplicationSummaryDto> listApplicationSummaries(Long userId) {
        return applicationRepository.findSummariesByUserId(userId);
    }

    private List<String> resolveSuccessStatuses(List<String> requested) {
        return (requested == null || requested.isEmpty()) ? DEFAULT_SUCCESS_STATUSES : requested;
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
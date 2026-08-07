package com.applyflow.tracker_api.services;

import com.applyflow.tracker_api.dtos.StatMetricDto;
import com.applyflow.tracker_api.repositories.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    @Transactional(readOnly = true)
    public List<StatMetricDto> getCvVariantStats(Long userId) {
        return calculateRates(analyticsRepository.getStatsByCvVariant(userId));
    }

    @Transactional(readOnly = true)
    public List<StatMetricDto> getLanguageStats(Long userId) {
        return calculateRates(analyticsRepository.getStatsByLanguage(userId));
    }

    @Transactional(readOnly = true)
    public List<StatMetricDto> getJobTitleStats(Long userId) {
        return calculateRates(analyticsRepository.getStatsByJobTitle(userId));
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
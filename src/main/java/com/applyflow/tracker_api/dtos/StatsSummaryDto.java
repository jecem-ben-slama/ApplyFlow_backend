package com.applyflow.tracker_api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsSummaryDto {
    private long totalApplications;
    private long sentCount;
    private double responseRate;
    private Double avgResponseDays;

    // active/stalled split — based on current Application.status
    private long activeCount;
    private long terminalCount;

    // never-viewed
    private long neverViewedCount;
    private double neverViewedRate;

    // interview -> offer, separate from overall response rate
    private long interviewedCount;
    private long offerCount;
    private Double interviewToOfferRate; // null if interviewedCount == 0

    private StatsPeriodSummaryDto currentPeriod;
    private StatsPeriodSummaryDto previousPeriod;

    public static StatsSummaryDto fromPeriod(StatsPeriodSummaryDto current, StatsPeriodSummaryDto previous) {
        return StatsSummaryDto.builder()
                .totalApplications(current != null ? current.getTotalApplications() : 0)
                .sentCount(current != null ? current.getSentCount() : 0)
                .responseRate(current != null ? current.getResponseRate() : 0.0)
                .avgResponseDays(current != null ? current.getAvgResponseDays() : null)
                .activeCount(current != null ? current.getActiveCount() : 0)
                .terminalCount(current != null ? current.getTerminalCount() : 0)
                .neverViewedCount(current != null ? current.getNeverViewedCount() : 0)
                .neverViewedRate(current != null ? current.getNeverViewedRate() : 0.0)
                .interviewedCount(current != null ? current.getInterviewedCount() : 0)
                .offerCount(current != null ? current.getOfferCount() : 0)
                .interviewToOfferRate(current != null ? current.getInterviewToOfferRate() : null)
                .currentPeriod(current)
                .previousPeriod(previous)
                .build();
    }
}
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
}
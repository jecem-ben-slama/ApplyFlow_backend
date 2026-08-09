package com.applyflow.tracker_api.dtos;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsPeriodSummaryDto {
    private long totalApplications;
    private long sentCount;
    private double responseRate;
    private Double avgResponseDays;
    private long activeCount;
    private long terminalCount;
    private long neverViewedCount;
    private double neverViewedRate;
    private long interviewedCount;
    private long offerCount;
    private Double interviewToOfferRate;
    private String periodLabel;
    public LocalDateTime minusDays(int i) {
        throw new UnsupportedOperationException("Unimplemented method 'minusDays'");
    }
    public LocalDateTime withHour(int i) {
        throw new UnsupportedOperationException("Unimplemented method 'withHour'");
    }
}

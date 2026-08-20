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
    private long respondedCount;
    private long viewedCount;
    private double responseRate;
    private Double avgResponseDays;
    private long activeCount;
    private long terminalCount;
    private long neverViewedCount;
    private double neverViewedRate;

    // "Ignored": currently sitting at VIEWED with nothing after it (no response,
    // no rejection, no progression). Safe to derive from CURRENT status because
    // ApplicationService backfills every skipped progression stage on a status
    // jump — so status can only still read VIEWED if nothing past it ever
    // happened.
    private long ignoredCount;
    private double ignoredRate; // ignoredCount / viewedCount, 0 if viewedCount == 0

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
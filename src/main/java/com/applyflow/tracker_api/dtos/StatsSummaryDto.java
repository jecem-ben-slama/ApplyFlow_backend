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
    private double responseRate; // 0.0 - 1.0
    private Double avgResponseDays; // null when no application has been responded to yet
}
package com.applyflow.tracker_api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsTrendResponseDto {
    private List<TrendPointDto> applicationsOverTime;
    private List<TrendPointDto> responseRateOverTime;
    private List<TrendPointDto> interviewToOfferRateOverTime;
    private List<TrendPointDto> rejectionTrendOverTime;
    private String granularity;
}

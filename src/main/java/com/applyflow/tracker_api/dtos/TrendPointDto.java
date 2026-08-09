package com.applyflow.tracker_api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendPointDto {
    private LocalDate date;
    private long value;
    private Double percent;
    private Long currentValue;
    private Long previousValue;
}

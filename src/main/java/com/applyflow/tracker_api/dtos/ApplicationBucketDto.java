package com.applyflow.tracker_api.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

// Minimal projection used by StatsService.getTrendData to fetch all
// applications for a date range in a single query, then bucket them by day
// in memory instead of issuing one query per day.
@Getter
@AllArgsConstructor
public class ApplicationBucketDto {
    private final Long id;
    private final LocalDateTime dateApplied;
    private final String status;
}
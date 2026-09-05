package com.applyflow.tracker_api.dtos;

import java.time.LocalDateTime;

public record StatsEventTimeDto(Long applicationId, String status, LocalDateTime occurredAt) {
}

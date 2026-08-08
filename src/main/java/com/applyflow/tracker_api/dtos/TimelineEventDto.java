package com.applyflow.tracker_api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineEventDto {
    private Long id;
    private Long applicationId;
    private String status;
    private String note;
    private LocalDateTime occurredAt;
    private String companyName;
    private String jobTitle;
}
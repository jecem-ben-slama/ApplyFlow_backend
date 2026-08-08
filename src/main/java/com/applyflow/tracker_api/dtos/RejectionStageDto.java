package com.applyflow.tracker_api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectionStageDto {
    private String stage; // "BEFORE_INTERVIEW" | "AFTER_INTERVIEW"
    private long count;
}
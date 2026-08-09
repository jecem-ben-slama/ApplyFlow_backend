package com.applyflow.tracker_api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatMetricDto {
    private String categoryName;         
    private long totalApplications;     
    private long successCount;          
    private double successRatePercentage; 
}
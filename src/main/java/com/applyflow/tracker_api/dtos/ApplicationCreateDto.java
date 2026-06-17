package com.applyflow.tracker_api.dtos;

import lombok.*;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationCreateDto {
    private String companyName;
    private String jobTitle;
    private String recipientEmail;
    private String language;
    private Long templateId;
    private Long cvVariantId;
    private Long userId;
    private Set<Long> skillIds;
    private String notes;
}
package com.applyflow.tracker_api.dtos;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponseDto {
    private Long id;
    private String companyName;
    private String jobTitle;
    private String recipientEmail;
    private String language;
    private String status;
    private String generatedSubject;
    private String generatedBody;
    private LocalDateTime dateApplied;
    private String notes;
    private Long templateId;
    private Long cvVariantId;
    private Long userId;
    private Set<Long> skillIds;
}
package com.applyflow.tracker_api.dtos;

import lombok.*;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationPresetDto {
    private Long id;
    private String name;
    private String jobTitle;
    private String language;
    private Long templateId;
    private Long cvVariantId;
    private Set<Long> skillIds;
    private String notes;
}
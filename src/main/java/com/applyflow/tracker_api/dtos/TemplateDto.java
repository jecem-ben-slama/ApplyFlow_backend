package com.applyflow.tracker_api.dtos;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateDto {
    private Long id;
    private String name;
    private String language;
    private Integer tier;
    private String subjectTemplate;
    private String bodyTemplate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId; // <-- Linked to single user ownership
}
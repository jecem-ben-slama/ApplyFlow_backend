package com.applyflow.tracker_api.dtos;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvVariantDto {
    private Long id;
    private String name;
    private String language;
    private String fileUrl;
    private LocalDateTime createdAt;
}
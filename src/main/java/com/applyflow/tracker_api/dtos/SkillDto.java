package com.applyflow.tracker_api.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillDto {
    private Long id;
    private String name;
    private String sentenceEn;
    private String sentenceFr;
    private Long userId; 
}
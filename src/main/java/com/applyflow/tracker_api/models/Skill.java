package com.applyflow.tracker_api.models;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

@Entity
@Table(name = "skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "technical_name", nullable = false, length = 100)
    private String technicalName;

    @Column(name = "sentence_en", nullable = false, columnDefinition = "TEXT")
    private String sentenceEn;

    @Column(name = "sentence_fr", nullable = false, columnDefinition = "TEXT")
    private String sentenceFr;

    @ManyToMany(mappedBy = "skills")
    private Set<Application> applications;
}
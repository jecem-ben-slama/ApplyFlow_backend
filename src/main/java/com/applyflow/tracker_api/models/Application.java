package com.applyflow.tracker_api.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "job_title", nullable = false, length = 150)
    private String jobTitle;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(length = 50)
    @Builder.Default
    private String status = "Compiled";

    @Column(name = "generated_subject", nullable = false, columnDefinition = "TEXT")
    private String generatedSubject;

    @Column(name = "generated_body", nullable = false, columnDefinition = "TEXT")
    private String generatedBody;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private Template template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_variant_id")
    private CvVariant cvVariant;

    @Column(name = "date_applied")
    @Builder.Default
    private LocalDateTime dateApplied = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "application_skills", joinColumns = @JoinColumn(name = "application_id"), inverseJoinColumns = @JoinColumn(name = "skill_id"))
    @Builder.Default
    private Set<Skill> skills = new HashSet<>();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
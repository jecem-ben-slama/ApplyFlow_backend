package com.applyflow.tracker_api.models;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Block deletion of a category if any skills are still assigned to it.
     * The service layer should check this and return a meaningful error to the
     * client.
     */
    @OneToMany(mappedBy = "category")
    @Builder.Default
    private List<Skill> skills = new ArrayList<>();
}
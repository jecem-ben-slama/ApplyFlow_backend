package com.applyflow.tracker_api.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.applyflow.tracker_api.config.EncryptedStringConverter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Null for guest sessions. Unique constraint still holds since Postgres
     * treats multiple NULLs as distinct — no collision risk between guests.
     */
    @Column(name = "google_sub", unique = true, length = 255)
    private String googleSub;

    @Column(unique = true, length = 255)
    private String email;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "picture_url", columnDefinition = "TEXT")
    private String pictureUrl;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "token_expiry")
    private LocalDateTime tokenExpiry;

    @Column(name = "deletion_requested_at")
    private LocalDateTime deletionRequestedAt;

    /**
     * True for guest sessions created via /api/auth/guest.
     * Flipped to false once the user links a Google account.
     */
    @Column(name = "is_guest", nullable = false)
    @Builder.Default
    private Boolean isGuest = false;

    /**
     * Random UUID identifying an anonymous guest session. Null once the
     * guest links a Google account (or was never a guest).
     */
    @Column(name = "guest_token", unique = true, length = 255)
    private String guestToken;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Application> applications = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CvVariant> cvVariants = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Skill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Template> templates = new ArrayList<>();

    /**
     * Categories are user-owned. Deleting the user cascades and removes all their
     * categories.
     * Deletion of a single category is blocked at the service layer if skills are
     * still linked.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Category> categories = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ApplicationPreset> presets = new ArrayList<>();

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
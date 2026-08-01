package com.example.WebSideProject.entity;

import com.example.WebSideProject.Enum.AgeGroup;
import com.example.WebSideProject.Enum.GenderType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_subscribed", columnList = "subscribed"),
                @Index(name = "idx_users_coders_user", columnList = "codersUserId")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true, length = 64)
    private String codersUserId;

    @Column(nullable = false)
    private boolean subscribed = true;

    @Column(unique = true, length = 64)
    private String unsubscribeToken;

    @Column(nullable = false)
    private String locationName = "서울특별시 중구";

    private Double latitude;
    private Double longitude;

    private int nx = 60;   // 기본값: 서울
    private int ny = 127;

    @Enumerated(EnumType.STRING)
    private AgeGroup ageGroup = AgeGroup.NONE;

    @Enumerated(EnumType.STRING)
    private GenderType gender = GenderType.NONE;

    @Column(nullable = false)
    private boolean morningEnabled = true;

    @Column(nullable = false)
    private boolean afternoonEnabled = false;

    @Column(nullable = false)
    private boolean eveningEnabled = false;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        normalizeStylePreference();
        ensureUnsubscribeToken();
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    @PostLoad
    protected void normalizeStylePreference() {
        ensureUnsubscribeToken();
        if (this.ageGroup == null) {
            this.ageGroup = AgeGroup.NONE;
        }
        if (this.gender == null) {
            this.gender = GenderType.NONE;
        }
    }

    @Builder
    public User(
            String name,
            String email,
            String codersUserId,
            String locationName,
            Double latitude,
            Double longitude,
            int nx,
            int ny,
            AgeGroup ageGroup,
            GenderType gender,
            boolean morningEnabled,
            boolean afternoonEnabled,
            boolean eveningEnabled
    ) {
        this.name = name;
        this.email = email;
        this.codersUserId = codersUserId;
        this.locationName = locationName == null || locationName.isBlank() ? "서울특별시 중구" : locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.nx = nx;
        this.ny = ny;
        this.ageGroup = ageGroup == null ? AgeGroup.NONE : ageGroup;
        this.gender = gender == null ? GenderType.NONE : gender;
        this.morningEnabled = morningEnabled;
        this.afternoonEnabled = afternoonEnabled;
        this.eveningEnabled = eveningEnabled;
        ensureUnsubscribeToken();
    }

    public void unsubscribe() { this.subscribed = false; }
    public void subscribe()   { this.subscribed = true; }

    public AgeGroup getAgeGroup() {
        return ageGroup == null ? AgeGroup.NONE : ageGroup;
    }

    public GenderType getGender() {
        return gender == null ? GenderType.NONE : gender;
    }

    public void updateLocation(
            String locationName,
            Double latitude,
            Double longitude,
            int nx,
            int ny
    ) {
        this.locationName = locationName == null || locationName.isBlank() ? this.locationName : locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.nx = nx;
        this.ny = ny;
    }

    public void updateStylePreference(AgeGroup ageGroup, GenderType gender) {
        this.ageGroup = ageGroup == null ? AgeGroup.NONE : ageGroup;
        this.gender = gender == null ? GenderType.NONE : gender;
    }

    public void updateNotificationTimes(
            boolean morningEnabled,
            boolean afternoonEnabled,
            boolean eveningEnabled
    ) {
        boolean noTimeSelected = !morningEnabled && !afternoonEnabled && !eveningEnabled;
        this.morningEnabled = morningEnabled || noTimeSelected;
        this.afternoonEnabled = afternoonEnabled;
        this.eveningEnabled = eveningEnabled;
    }

    public void claimCodersIdentity(String codersUserId) {
        if (this.codersUserId == null || this.codersUserId.isBlank()) {
            this.codersUserId = codersUserId;
        }
    }

    public void ensureUnsubscribeToken() {
        if (this.unsubscribeToken == null || this.unsubscribeToken.isBlank()) {
            this.unsubscribeToken = UUID.randomUUID().toString().replace("-", "");
        }
    }
}

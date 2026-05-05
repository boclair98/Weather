package com.example.WebSideProject.entity;

import com.example.WebSideProject.Enum.AgeGroup;
import com.example.WebSideProject.Enum.GenderType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
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

    @Column(nullable = false)
    private boolean subscribed = true;

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
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    @PostLoad
    protected void normalizeStylePreference() {
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
}

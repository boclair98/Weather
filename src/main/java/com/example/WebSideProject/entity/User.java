package com.example.WebSideProject.entity;

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
        this.createdAt = LocalDateTime.now();
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
        this.morningEnabled = morningEnabled;
        this.afternoonEnabled = afternoonEnabled;
        this.eveningEnabled = eveningEnabled;
    }

    public void unsubscribe() { this.subscribed = false; }
    public void subscribe()   { this.subscribed = true; }
}

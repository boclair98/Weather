package com.example.WebSideProject.entity;

import com.example.WebSideProject.Enum.AgeGroup;
import com.example.WebSideProject.Enum.ActivityType;
import com.example.WebSideProject.Enum.GenderType;
import com.example.WebSideProject.Enum.TemperatureSensitivity;
import com.example.WebSideProject.Enum.WeatherPeriod;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_subscribed", columnList = "subscribed"),
                @Index(name = "idx_users_coders_user", columnList = "codersUserId"),
                @Index(name = "idx_users_owner", columnList = "ownerId")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    public static final LocalTime DEFAULT_MORNING_TIME = LocalTime.of(6, 30);
    public static final LocalTime DEFAULT_AFTERNOON_TIME = LocalTime.of(11, 30);
    public static final LocalTime DEFAULT_EVENING_TIME = LocalTime.of(18, 30);

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

    @Column(length = 64)
    private String ownerId;

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

    @Enumerated(EnumType.STRING)
    @Column
    private TemperatureSensitivity temperatureSensitivity = TemperatureSensitivity.NONE;

    @Enumerated(EnumType.STRING)
    @Column
    private ActivityType activityType = ActivityType.DAILY;

    @Column
    private Boolean smartAlertEnabled = false;

    @Column
    private Boolean rainAlertEnabled = true;

    @Column
    private Boolean temperatureAlertEnabled = true;

    @Column
    private Boolean airQualityAlertEnabled = true;

    @Column
    private Boolean windAlertEnabled = true;

    @Column(length = 160)
    private String lastSmartAlertFingerprint;

    private LocalDateTime lastSmartAlertAt;

    @Column(nullable = false)
    private boolean morningEnabled = true;

    @Column(nullable = false)
    private boolean afternoonEnabled = false;

    @Column(nullable = false)
    private boolean eveningEnabled = false;

    @Column(nullable = false)
    private LocalTime morningTime = DEFAULT_MORNING_TIME;

    @Column(nullable = false)
    private LocalTime afternoonTime = DEFAULT_AFTERNOON_TIME;

    @Column(nullable = false)
    private LocalTime eveningTime = DEFAULT_EVENING_TIME;

    private LocalDate lastMorningMailDate;

    private LocalDate lastAfternoonMailDate;

    private LocalDate lastEveningMailDate;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(length = 32)
    private String privacyConsentVersion;

    private LocalDateTime privacyConsentAt;

    private LocalDateTime unsubscribedAt;

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
        if (this.temperatureSensitivity == null) {
            this.temperatureSensitivity = TemperatureSensitivity.NONE;
        }
        if (this.activityType == null) {
            this.activityType = ActivityType.DAILY;
        }
        if (this.smartAlertEnabled == null) this.smartAlertEnabled = false;
        if (this.rainAlertEnabled == null) this.rainAlertEnabled = true;
        if (this.temperatureAlertEnabled == null) this.temperatureAlertEnabled = true;
        if (this.airQualityAlertEnabled == null) this.airQualityAlertEnabled = true;
        if (this.windAlertEnabled == null) this.windAlertEnabled = true;
        this.morningTime = normalizeTime(this.morningTime, DEFAULT_MORNING_TIME);
        this.afternoonTime = normalizeTime(this.afternoonTime, DEFAULT_AFTERNOON_TIME);
        this.eveningTime = normalizeTime(this.eveningTime, DEFAULT_EVENING_TIME);
    }

    @Builder
    public User(
            String name,
            String email,
            String codersUserId,
            String ownerId,
            String locationName,
            Double latitude,
            Double longitude,
            int nx,
            int ny,
            AgeGroup ageGroup,
            GenderType gender,
            TemperatureSensitivity temperatureSensitivity,
            ActivityType activityType,
            Boolean smartAlertEnabled,
            Boolean rainAlertEnabled,
            Boolean temperatureAlertEnabled,
            Boolean airQualityAlertEnabled,
            Boolean windAlertEnabled,
            boolean morningEnabled,
            boolean afternoonEnabled,
            boolean eveningEnabled,
            LocalTime morningTime,
            LocalTime afternoonTime,
            LocalTime eveningTime
    ) {
        this.name = name;
        this.email = email;
        this.codersUserId = codersUserId;
        this.ownerId = ownerId;
        this.locationName = locationName == null || locationName.isBlank() ? "서울특별시 중구" : locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.nx = nx;
        this.ny = ny;
        this.ageGroup = ageGroup == null ? AgeGroup.NONE : ageGroup;
        this.gender = gender == null ? GenderType.NONE : gender;
        this.temperatureSensitivity = temperatureSensitivity == null
                ? TemperatureSensitivity.NONE : temperatureSensitivity;
        this.activityType = activityType == null ? ActivityType.DAILY : activityType;
        this.smartAlertEnabled = Boolean.TRUE.equals(smartAlertEnabled);
        this.rainAlertEnabled = rainAlertEnabled == null || rainAlertEnabled;
        this.temperatureAlertEnabled = temperatureAlertEnabled == null || temperatureAlertEnabled;
        this.airQualityAlertEnabled = airQualityAlertEnabled == null || airQualityAlertEnabled;
        this.windAlertEnabled = windAlertEnabled == null || windAlertEnabled;
        this.morningEnabled = morningEnabled;
        this.afternoonEnabled = afternoonEnabled;
        this.eveningEnabled = eveningEnabled;
        this.morningTime = normalizeTime(morningTime, DEFAULT_MORNING_TIME);
        this.afternoonTime = normalizeTime(afternoonTime, DEFAULT_AFTERNOON_TIME);
        this.eveningTime = normalizeTime(eveningTime, DEFAULT_EVENING_TIME);
        ensureUnsubscribeToken();
    }

    public void unsubscribe() {
        this.subscribed = false;
        this.unsubscribedAt = LocalDateTime.now();
    }

    public void subscribe() {
        this.subscribed = true;
        this.unsubscribedAt = null;
    }

    public void recordPrivacyConsent(String version) {
        this.privacyConsentVersion = version;
        this.privacyConsentAt = LocalDateTime.now();
    }

    public AgeGroup getAgeGroup() {
        return ageGroup == null ? AgeGroup.NONE : ageGroup;
    }

    public GenderType getGender() {
        return gender == null ? GenderType.NONE : gender;
    }

    public TemperatureSensitivity getTemperatureSensitivity() {
        return temperatureSensitivity == null ? TemperatureSensitivity.NONE : temperatureSensitivity;
    }

    public ActivityType getActivityType() {
        return activityType == null ? ActivityType.DAILY : activityType;
    }

    public boolean isSmartAlertEnabled() {
        return Boolean.TRUE.equals(smartAlertEnabled);
    }

    public boolean isRainAlertEnabled() {
        return rainAlertEnabled == null || rainAlertEnabled;
    }

    public boolean isTemperatureAlertEnabled() {
        return temperatureAlertEnabled == null || temperatureAlertEnabled;
    }

    public boolean isAirQualityAlertEnabled() {
        return airQualityAlertEnabled == null || airQualityAlertEnabled;
    }

    public boolean isWindAlertEnabled() {
        return windAlertEnabled == null || windAlertEnabled;
    }

    public void updateName(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
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

    public void updateStylePreference(
            AgeGroup ageGroup,
            GenderType gender,
            TemperatureSensitivity temperatureSensitivity,
            ActivityType activityType
    ) {
        updateStylePreference(ageGroup, gender);
        this.temperatureSensitivity = temperatureSensitivity == null
                ? TemperatureSensitivity.NONE : temperatureSensitivity;
        this.activityType = activityType == null ? ActivityType.DAILY : activityType;
    }

    public void updateNotificationTimes(
            boolean morningEnabled,
            boolean afternoonEnabled,
            boolean eveningEnabled
    ) {
        updateNotificationTimes(
                morningEnabled,
                afternoonEnabled,
                eveningEnabled,
                this.morningTime,
                this.afternoonTime,
                this.eveningTime
        );
    }

    public void updateNotificationTimes(
            boolean morningEnabled,
            boolean afternoonEnabled,
            boolean eveningEnabled,
            LocalTime morningTime,
            LocalTime afternoonTime,
            LocalTime eveningTime
    ) {
        boolean noTimeSelected = !morningEnabled && !afternoonEnabled && !eveningEnabled;
        this.morningEnabled = morningEnabled || noTimeSelected;
        this.afternoonEnabled = afternoonEnabled;
        this.eveningEnabled = eveningEnabled;
        this.morningTime = normalizeTime(morningTime, DEFAULT_MORNING_TIME);
        this.afternoonTime = normalizeTime(afternoonTime, DEFAULT_AFTERNOON_TIME);
        this.eveningTime = normalizeTime(eveningTime, DEFAULT_EVENING_TIME);
    }

    public LocalTime getNotificationTime(WeatherPeriod period) {
        return switch (period) {
            case MORNING -> morningTime;
            case AFTERNOON -> afternoonTime;
            case EVENING -> eveningTime;
        };
    }

    public boolean isEnabledFor(WeatherPeriod period) {
        return switch (period) {
            case MORNING -> morningEnabled;
            case AFTERNOON -> afternoonEnabled;
            case EVENING -> eveningEnabled;
        };
    }

    /**
     * Claims one scheduled slot for a day. The scheduler can run repeatedly
     * without sending the same period twice when multiple app instances are up.
     */
    public boolean claimScheduledMail(WeatherPeriod period, LocalDate date) {
        if (!subscribed || !isEnabledFor(period) || date == null) {
            return false;
        }
        switch (period) {
            case MORNING -> {
                if (date.equals(lastMorningMailDate)) return false;
                lastMorningMailDate = date;
            }
            case AFTERNOON -> {
                if (date.equals(lastAfternoonMailDate)) return false;
                lastAfternoonMailDate = date;
            }
            case EVENING -> {
                if (date.equals(lastEveningMailDate)) return false;
                lastEveningMailDate = date;
            }
        }
        return true;
    }

    public void claimCodersIdentity(String codersUserId) {
        if (codersUserId == null || codersUserId.isBlank()) {
            return;
        }
        if (this.ownerId != null && !this.ownerId.isBlank()) {
            return;
        }
        if (this.codersUserId != null
                && !this.codersUserId.isBlank()
                && !this.codersUserId.equals(codersUserId)) {
            return;
        }
        this.ownerId = codersUserId;
    }

    public void updateSmartAlerts(
            boolean smartAlertEnabled,
            boolean rainAlertEnabled,
            boolean temperatureAlertEnabled,
            boolean airQualityAlertEnabled,
            boolean windAlertEnabled
    ) {
        this.smartAlertEnabled = smartAlertEnabled;
        this.rainAlertEnabled = rainAlertEnabled;
        this.temperatureAlertEnabled = temperatureAlertEnabled;
        this.airQualityAlertEnabled = airQualityAlertEnabled;
        this.windAlertEnabled = windAlertEnabled;
    }

    public boolean hasReceivedSmartAlert(String fingerprint) {
        return fingerprint != null && fingerprint.equals(lastSmartAlertFingerprint);
    }

    public void markSmartAlertSent(String fingerprint) {
        this.lastSmartAlertFingerprint = fingerprint;
        this.lastSmartAlertAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(String codersUserId) {
        return codersUserId != null
                && (codersUserId.equals(ownerId) || codersUserId.equals(this.codersUserId));
    }

    public void ensureUnsubscribeToken() {
        if (this.unsubscribeToken == null || this.unsubscribeToken.isBlank()) {
            this.unsubscribeToken = UUID.randomUUID().toString().replace("-", "");
        }
    }

    private LocalTime normalizeTime(LocalTime value, LocalTime fallback) {
        if (value == null) return fallback;
        return value.withSecond(0).withNano(0);
    }
}

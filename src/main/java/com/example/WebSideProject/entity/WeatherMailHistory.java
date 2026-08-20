package com.example.WebSideProject.entity;

import com.example.WebSideProject.Enum.MailSendStatus;
import com.example.WebSideProject.Enum.WeatherPeriod;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "weather_mail_histories",
        indexes = {
                @Index(name = "idx_mail_history_sent_at", columnList = "sentAt"),
                @Index(name = "idx_mail_history_email_sent_at", columnList = "userEmail,sentAt")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeatherMailHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String locationName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WeatherPeriod period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MailSendStatus status;

    private String forecastDate;

    private String forecastTime;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Builder
    public WeatherMailHistory(
            String userEmail,
            String locationName,
            WeatherPeriod period,
            MailSendStatus status,
            String forecastDate,
            String forecastTime,
            String errorMessage
    ) {
        this.userEmail = userEmail;
        this.locationName = locationName;
        this.period = period;
        this.status = status;
        this.forecastDate = forecastDate;
        this.forecastTime = forecastTime;
        this.errorMessage = errorMessage;
        this.sentAt = LocalDateTime.now();
    }
}

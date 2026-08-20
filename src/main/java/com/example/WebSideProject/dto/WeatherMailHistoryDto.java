package com.example.WebSideProject.dto;

import com.example.WebSideProject.Enum.MailSendStatus;
import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.entity.WeatherMailHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class WeatherMailHistoryDto {

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String userEmail;
        private String locationName;
        private WeatherPeriod period;
        private MailSendStatus status;
        private String forecastDate;
        private String forecastTime;
        private String errorMessage;
        private LocalDateTime sentAt;

        public static Response from(WeatherMailHistory history) {
            return Response.builder()
                    .id(history.getId())
                    .userEmail(history.getUserEmail())
                    .locationName(history.getLocationName())
                    .period(history.getPeriod())
                    .status(history.getStatus())
                    .forecastDate(history.getForecastDate())
                    .forecastTime(history.getForecastTime())
                    .errorMessage(history.getErrorMessage())
                    .sentAt(history.getSentAt())
                    .build();
        }
    }
}

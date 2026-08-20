package com.example.WebSideProject.dto;

import com.example.WebSideProject.Enum.ActivityType;
import com.example.WebSideProject.Enum.AgeGroup;
import com.example.WebSideProject.Enum.GenderType;
import com.example.WebSideProject.Enum.TemperatureSensitivity;

/**
 * Immutable request context shared by every personalized weather response.
 * Keeping the four optional values together prevents controller/service drift.
 */
public record WeatherProfile(
        AgeGroup ageGroup,
        GenderType gender,
        TemperatureSensitivity temperatureSensitivity,
        ActivityType activityType
) {

    public WeatherProfile {
        ageGroup = ageGroup == null ? AgeGroup.NONE : ageGroup;
        gender = gender == null ? GenderType.NONE : gender;
        temperatureSensitivity = temperatureSensitivity == null
                ? TemperatureSensitivity.NONE : temperatureSensitivity;
        activityType = activityType == null ? ActivityType.DAILY : activityType;
    }

    public static WeatherProfile defaults() {
        return new WeatherProfile(
                AgeGroup.NONE,
                GenderType.NONE,
                TemperatureSensitivity.NONE,
                ActivityType.DAILY
        );
    }
}

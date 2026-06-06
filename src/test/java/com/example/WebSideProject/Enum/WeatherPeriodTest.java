package com.example.WebSideProject.Enum;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherPeriodTest {

    @Test
    void eveningNotificationUsesNightForecast() {
        assertThat(WeatherPeriod.EVENING.getSendTime()).isEqualTo(LocalTime.of(18, 30));
        assertThat(WeatherPeriod.EVENING.getTargetTime()).isEqualTo("2100");
        assertThat(WeatherPeriod.EVENING.getTargetHour()).isEqualTo(21);
    }

    @Test
    void unknownLabelFallsBackToMorning() {
        assertThat(WeatherPeriod.fromLabel("없는 시간대")).isEqualTo(WeatherPeriod.MORNING);
    }
}

package com.example.WebSideProject.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherApiHealthIndicatorTest {

    @Test
    void reportsDownWhenRequiredWeatherKeyIsMissing() {
        WeatherApiHealthIndicator indicator = new WeatherApiHealthIndicator(" ");

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(indicator.health().getDetails()).containsKey("configuration");
    }

    @Test
    void reportsUpWhenWeatherKeyIsConfigured() {
        WeatherApiHealthIndicator indicator = new WeatherApiHealthIndicator("configured-key");

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }
}

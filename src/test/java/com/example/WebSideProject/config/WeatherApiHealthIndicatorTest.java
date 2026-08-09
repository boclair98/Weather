package com.example.WebSideProject.config;

import com.example.WebSideProject.service.ExternalApiGuard;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WeatherApiHealthIndicatorTest {

    @Test
    void reportsDownWhenRequiredWeatherKeyIsMissing() {
        WeatherApiHealthIndicator indicator = new WeatherApiHealthIndicator(" ", mock(ExternalApiGuard.class));

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(indicator.health().getDetails()).containsKey("configuration");
    }

    @Test
    void reportsUpWhenWeatherKeyIsConfigured() {
        WeatherApiHealthIndicator indicator = new WeatherApiHealthIndicator(
                "configured-key", mock(ExternalApiGuard.class)
        );

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void reportsDegradedWhenForecastCircuitIsOpen() {
        ExternalApiGuard guard = mock(ExternalApiGuard.class);
        when(guard.isCircuitOpen("kma-forecast")).thenReturn(true);
        WeatherApiHealthIndicator indicator = new WeatherApiHealthIndicator("configured-key", guard);

        assertThat(indicator.health().getStatus().getCode()).isEqualTo("DEGRADED");
        assertThat(indicator.health().getDetails()).containsKey("fallback");
    }
}

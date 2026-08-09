package com.example.WebSideProject.service;

import com.example.WebSideProject.Enum.AgeGroup;
import com.example.WebSideProject.Enum.ActivityType;
import com.example.WebSideProject.Enum.GenderType;
import com.example.WebSideProject.Enum.TemperatureSensitivity;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WeatherPlannerServiceTest {

    @Test
    void unwrapsCacheFailureSoProviderOutageRemainsServiceUnavailable() {
        WeatherService weatherService = mock(WeatherService.class);
        IllegalStateException providerFailure = new IllegalStateException("원천 예보 장애");
        Cache.ValueRetrievalException cacheFailure = new Cache.ValueRetrievalException(
                "planner-source", () -> null, providerFailure
        );
        when(weatherService.getPlannerDailyWeatherList(anyInt(), anyInt(), anyString()))
                .thenThrow(cacheFailure);

        WeatherPlannerService service = new WeatherPlannerService(
                weatherService,
                Runnable::run,
                new SimpleMeterRegistry()
        );

        assertThatThrownBy(() -> service.getPlanner(
                60, 127, "을지로3가",
                AgeGroup.NONE, GenderType.NONE,
                TemperatureSensitivity.NONE, ActivityType.DAILY
        ))
                .isSameAs(providerFailure);
    }
}

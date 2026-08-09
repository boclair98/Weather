package com.example.WebSideProject.service;

import com.example.WebSideProject.Enum.AgeGroup;
import com.example.WebSideProject.Enum.ActivityType;
import com.example.WebSideProject.Enum.GenderType;
import com.example.WebSideProject.Enum.TemperatureSensitivity;
import com.example.WebSideProject.dto.DailyWeatherDto;
import com.example.WebSideProject.dto.WeatherPlannerDto;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Service
public class WeatherPlannerService {

    private final WeatherService weatherService;
    private final Executor plannerExecutor;
    private final MeterRegistry meterRegistry;

    public WeatherPlannerService(
            WeatherService weatherService,
            @Qualifier("plannerExecutor") Executor plannerExecutor,
            MeterRegistry meterRegistry
    ) {
        this.weatherService = weatherService;
        this.plannerExecutor = plannerExecutor;
        this.meterRegistry = meterRegistry;
    }

    @Cacheable(
            cacheNames = "weather",
            key = "'planner:' + #nx + ':' + #ny + ':' + (#locationName == null ? '' : #locationName)"
                    + " + ':' + #ageGroup.name() + ':' + #gender.name()"
                    + " + ':' + #temperatureSensitivity.name() + ':' + #activityType.name()",
            sync = true
    )
    public WeatherPlannerDto getPlanner(
            int nx,
            int ny,
            String locationName,
            AgeGroup ageGroup,
            GenderType gender,
            TemperatureSensitivity temperatureSensitivity,
            ActivityType activityType
    ) {
        Timer.Sample timer = Timer.start(meterRegistry);
        boolean success = false;
        try {
            List<CompletableFuture<DailyWeatherDto>> futures = List.of(0, 1, 2).stream()
                    .map(dayOffset -> CompletableFuture.supplyAsync(
                            () -> weatherService.getDailyWeather(nx, ny, locationName, dayOffset)
                                    .withStylePreference(
                                            ageGroup, gender, temperatureSensitivity, activityType
                                    ),
                            plannerExecutor
                    ))
                    .toList();
            List<DailyWeatherDto> forecasts = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
            WeatherPlannerDto planner = WeatherPlannerDto.from(locationName, forecasts);
            success = true;
            return planner;
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("3일 예보를 만드는 중 오류가 발생했습니다.", cause);
        } finally {
            timer.stop(Timer.builder("weather.planner.generation")
                    .description("Cold-cache three-day weather planner generation time")
                    .tag("status", success ? "success" : "failure")
                    .register(meterRegistry));
        }
    }
}

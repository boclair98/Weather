package com.example.WebSideProject.controller;

import com.example.WebSideProject.Enum.AgeGroup;
import com.example.WebSideProject.Enum.ActivityType;
import com.example.WebSideProject.Enum.GenderType;
import com.example.WebSideProject.Enum.TemperatureSensitivity;
import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.dto.DailyWeatherDto;
import com.example.WebSideProject.dto.WeatherDto;
import com.example.WebSideProject.dto.WeatherDecisionDto;
import com.example.WebSideProject.dto.HourlyWeatherDto;
import com.example.WebSideProject.dto.WeatherPlannerDto;
import com.example.WebSideProject.service.WeatherPlannerService;
import com.example.WebSideProject.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private static final CacheControl WEATHER_CACHE = CacheControl
            .maxAge(5, TimeUnit.MINUTES)
            .staleWhileRevalidate(10, TimeUnit.MINUTES)
            .staleIfError(30, TimeUnit.MINUTES)
            .cachePublic();

    private final WeatherService weatherService;
    private final WeatherPlannerService weatherPlannerService;

    @GetMapping
    public ResponseEntity<WeatherDto> getWeather(
            @RequestParam(defaultValue = "60") int nx,
            @RequestParam(defaultValue = "127") int ny,
            @RequestParam(defaultValue = "MORNING") WeatherPeriod period,
            @RequestParam(required = false) String locationName,
            @RequestParam(defaultValue = "NONE") AgeGroup ageGroup,
            @RequestParam(defaultValue = "NONE") GenderType gender,
            @RequestParam(defaultValue = "NONE") TemperatureSensitivity temperatureSensitivity,
            @RequestParam(defaultValue = "DAILY") ActivityType activityType
    ) {
        return ResponseEntity.ok()
                .cacheControl(WEATHER_CACHE)
                .body(weatherService.getWeather(nx, ny, period, locationName)
                        .withStylePreference(ageGroup, gender, temperatureSensitivity, activityType));
    }

    @GetMapping("/daily")
    public ResponseEntity<DailyWeatherDto> getDailyWeather(
            @RequestParam(defaultValue = "60") int nx,
            @RequestParam(defaultValue = "127") int ny,
            @RequestParam(required = false) String locationName,
            @RequestParam(defaultValue = "NONE") AgeGroup ageGroup,
            @RequestParam(defaultValue = "NONE") GenderType gender,
            @RequestParam(defaultValue = "NONE") TemperatureSensitivity temperatureSensitivity,
            @RequestParam(defaultValue = "DAILY") ActivityType activityType,
            @RequestParam(defaultValue = "0") int dayOffset
    ) {
        return ResponseEntity.ok()
                .cacheControl(WEATHER_CACHE)
                .body(weatherService.getDailyWeather(nx, ny, locationName, dayOffset)
                        .withStylePreference(ageGroup, gender, temperatureSensitivity, activityType));
    }

    @GetMapping("/planner")
    public ResponseEntity<WeatherPlannerDto> getPlanner(
            @RequestParam(defaultValue = "60") int nx,
            @RequestParam(defaultValue = "127") int ny,
            @RequestParam(required = false) String locationName,
            @RequestParam(defaultValue = "NONE") AgeGroup ageGroup,
            @RequestParam(defaultValue = "NONE") GenderType gender,
            @RequestParam(defaultValue = "NONE") TemperatureSensitivity temperatureSensitivity,
            @RequestParam(defaultValue = "DAILY") ActivityType activityType
    ) {
        return ResponseEntity.ok()
                .cacheControl(WEATHER_CACHE)
                .body(weatherPlannerService.getPlanner(
                        nx, ny, locationName, ageGroup, gender, temperatureSensitivity, activityType
                ));
    }

    @GetMapping("/hourly")
    public ResponseEntity<HourlyWeatherDto> getHourlyWeather(
            @RequestParam(defaultValue = "60") int nx,
            @RequestParam(defaultValue = "127") int ny,
            @RequestParam(required = false) String locationName,
            @RequestParam(defaultValue = "0") int dayOffset
    ) {
        return ResponseEntity.ok()
                .cacheControl(WEATHER_CACHE)
                .body(weatherService.getHourlyWeather(nx, ny, locationName, dayOffset));
    }

    @GetMapping("/decision-window")
    public ResponseEntity<WeatherDecisionDto> getDecisionWindow(
            @RequestParam(defaultValue = "60") int nx,
            @RequestParam(defaultValue = "127") int ny,
            @RequestParam(required = false) String locationName,
            @RequestParam(required = false) String targetDate,
            @RequestParam(defaultValue = "18:00") String targetTime,
            @RequestParam(defaultValue = "90") int flexMinutes,
            @RequestParam(defaultValue = "60") int durationMinutes
    ) {
        return ResponseEntity.ok()
                .cacheControl(WEATHER_CACHE)
                .body(weatherService.getDecisionWindow(
                        nx, ny, locationName, targetDate, targetTime, flexMinutes, durationMinutes
                ));
    }

    @GetMapping("/test")
    public ResponseEntity<WeatherDto> getWeatherForBase(
            @RequestParam(defaultValue = "60") int nx,
            @RequestParam(defaultValue = "127") int ny,
            @RequestParam(defaultValue = "MORNING") WeatherPeriod period,
            @RequestParam(required = false) String locationName,
            @RequestParam(defaultValue = "NONE") AgeGroup ageGroup,
            @RequestParam(defaultValue = "NONE") GenderType gender,
            @RequestParam(defaultValue = "NONE") TemperatureSensitivity temperatureSensitivity,
            @RequestParam(defaultValue = "DAILY") ActivityType activityType,
            @RequestParam String baseDate,
            @RequestParam String baseTime
    ) {
        WeatherDto weather = weatherService.getWeatherForBase(
                nx, ny, period, locationName, baseDate, baseTime
        ).withStylePreference(ageGroup, gender, temperatureSensitivity, activityType);
        return ResponseEntity.ok(weather);
    }
}

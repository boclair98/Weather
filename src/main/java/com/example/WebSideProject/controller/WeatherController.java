package com.example.WebSideProject.controller;

import com.example.WebSideProject.Enum.AgeGroup;
import com.example.WebSideProject.Enum.GenderType;
import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.dto.DailyWeatherDto;
import com.example.WebSideProject.dto.WeatherDto;
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

    private final WeatherService weatherService;

    @GetMapping
    public ResponseEntity<WeatherDto> getWeather(
            @RequestParam(defaultValue = "60") int nx,
            @RequestParam(defaultValue = "127") int ny,
            @RequestParam(defaultValue = "MORNING") WeatherPeriod period,
            @RequestParam(required = false) String locationName,
            @RequestParam(defaultValue = "NONE") AgeGroup ageGroup,
            @RequestParam(defaultValue = "NONE") GenderType gender
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(weatherService.getWeather(nx, ny, period, locationName).withStylePreference(ageGroup, gender));
    }

    @GetMapping("/daily")
    public ResponseEntity<DailyWeatherDto> getDailyWeather(
            @RequestParam(defaultValue = "60") int nx,
            @RequestParam(defaultValue = "127") int ny,
            @RequestParam(required = false) String locationName,
            @RequestParam(defaultValue = "NONE") AgeGroup ageGroup,
            @RequestParam(defaultValue = "NONE") GenderType gender
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(weatherService.getDailyWeather(nx, ny, locationName)
                        .withStylePreference(ageGroup, gender));
    }

    @GetMapping("/test")
    public ResponseEntity<WeatherDto> getWeatherForBase(
            @RequestParam(defaultValue = "60") int nx,
            @RequestParam(defaultValue = "127") int ny,
            @RequestParam(defaultValue = "MORNING") WeatherPeriod period,
            @RequestParam(required = false) String locationName,
            @RequestParam(defaultValue = "NONE") AgeGroup ageGroup,
            @RequestParam(defaultValue = "NONE") GenderType gender,
            @RequestParam String baseDate,
            @RequestParam String baseTime
    ) {
        WeatherDto weather = weatherService.getWeatherForBase(
                nx, ny, period, locationName, baseDate, baseTime
        ).withStylePreference(ageGroup, gender);
        return ResponseEntity.ok(weather);
    }
}

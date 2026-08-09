package com.example.WebSideProject.controller;

import com.example.WebSideProject.Enum.AgeGroup;
import com.example.WebSideProject.Enum.ActivityType;
import com.example.WebSideProject.Enum.GenderType;
import com.example.WebSideProject.Enum.TemperatureSensitivity;
import com.example.WebSideProject.config.RequestIdFilter;
import com.example.WebSideProject.dto.InstitutionalBriefingDto;
import com.example.WebSideProject.dto.WeatherPlannerDto;
import com.example.WebSideProject.service.WeatherPlannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class InstitutionalWeatherController {
    private static final CacheControl BRIEFING_CACHE = CacheControl
            .maxAge(5, TimeUnit.MINUTES)
            .staleWhileRevalidate(10, TimeUnit.MINUTES)
            .staleIfError(30, TimeUnit.MINUTES)
            .cachePublic();

    private final WeatherPlannerService weatherPlannerService;

    @GetMapping("/briefing")
    public ResponseEntity<InstitutionalBriefingDto> getBriefing(
            @RequestParam(defaultValue = "60") int nx,
            @RequestParam(defaultValue = "127") int ny,
            @RequestParam(required = false) String locationName,
            @RequestParam(defaultValue = "NONE") AgeGroup ageGroup,
            @RequestParam(defaultValue = "NONE") GenderType gender,
            @RequestParam(defaultValue = "NONE") TemperatureSensitivity temperatureSensitivity,
            @RequestParam(defaultValue = "DAILY") ActivityType activityType
    ) {
        String requestId = RequestIdFilter.currentOrNew();
        WeatherPlannerDto planner = weatherPlannerService.getPlanner(
                nx, ny, locationName, ageGroup, gender, temperatureSensitivity, activityType
        );
        InstitutionalBriefingDto response = InstitutionalBriefingDto.from(requestId, planner);
        return ResponseEntity.ok()
                .cacheControl(BRIEFING_CACHE)
                .header("X-Schema-Version", response.schemaVersion())
                .header("X-Data-Source", "KMA_VILAGE_FORECAST")
                .header("X-Data-Freshness", response.provenance().freshness())
                .header("X-Data-Quality", response.provenance().qualityStatus())
                .body(response);
    }
}

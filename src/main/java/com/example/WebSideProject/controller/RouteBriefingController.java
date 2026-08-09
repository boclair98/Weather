package com.example.WebSideProject.controller;

import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.dto.RouteBriefingDto;
import com.example.WebSideProject.service.RouteBriefingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteBriefingController {

    private final RouteBriefingService routeBriefingService;

    @GetMapping("/briefing")
    public ResponseEntity<RouteBriefingDto> getBriefing(
            @RequestParam String originQuery,
            @RequestParam String destinationQuery,
            @RequestParam(defaultValue = "MORNING") WeatherPeriod period
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                .body(routeBriefingService.getBriefing(originQuery, destinationQuery, period));
    }
}

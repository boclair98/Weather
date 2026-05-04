package com.example.WebSideProject.controller;

import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.dto.WeatherDto;
import com.example.WebSideProject.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping
    public WeatherDto getWeather(
            @RequestParam(defaultValue = "60") int nx,
            @RequestParam(defaultValue = "127") int ny,
            @RequestParam(defaultValue = "MORNING") WeatherPeriod period
    ) {
        return weatherService.getWeather(nx, ny, period);
    }
}

package com.example.WebSideProject.controller;

import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.scheduler.WeatherMailScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/weather-mails")
@RequiredArgsConstructor
public class WeatherMailController {

    private final WeatherMailScheduler weatherMailScheduler;

    @PostMapping("/send-now")
    public ResponseEntity<Map<String, String>> sendNow(
            @RequestParam(defaultValue = "MORNING") WeatherPeriod period
    ) {
        weatherMailScheduler.sendWeatherMailByPeriod(period);
        return ResponseEntity.ok(Map.of("message", period.getLabel() + " 날씨 메일 발송을 시작했습니다."));
    }
}

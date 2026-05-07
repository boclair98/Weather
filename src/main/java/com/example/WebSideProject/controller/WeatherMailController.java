package com.example.WebSideProject.controller;

import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.dto.WeatherMailHistoryDto;
import com.example.WebSideProject.scheduler.WeatherMailScheduler;
import com.example.WebSideProject.service.WeatherMailHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/weather-mails")
@RequiredArgsConstructor
public class WeatherMailController {

    private final WeatherMailScheduler weatherMailScheduler;
    private final WeatherMailHistoryService weatherMailHistoryService;

    @PostMapping("/send-now")
    public ResponseEntity<Map<String, String>> sendNow(
            @RequestParam(defaultValue = "MORNING") WeatherPeriod period
    ) {
        weatherMailScheduler.sendWeatherMailByPeriod(period);
        return ResponseEntity.ok(Map.of("message", period.getLabel() + " 날씨 메일 발송을 시작했습니다."));
    }

    @PostMapping("/send-all")
    public ResponseEntity<Map<String, String>> sendAll() {
        weatherMailScheduler.sendAllWeatherMails();
        return ResponseEntity.ok(Map.of("message", "아침/점심/저녁 날씨 메일 전체 발송을 시작했습니다."));
    }

    @GetMapping("/histories")
    public ResponseEntity<List<WeatherMailHistoryDto.Response>> getHistories(
            @RequestParam(required = false) String email
    ) {
        return ResponseEntity.ok(weatherMailHistoryService.getRecentHistories(email));
    }
}

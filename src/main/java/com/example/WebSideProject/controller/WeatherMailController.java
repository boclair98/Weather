package com.example.WebSideProject.controller;

import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.dto.WeatherDto;
import com.example.WebSideProject.dto.WeatherMailHistoryDto;
import com.example.WebSideProject.entity.User;
import com.example.WebSideProject.scheduler.WeatherMailScheduler;
import com.example.WebSideProject.service.MailService;
import com.example.WebSideProject.service.UserService;
import com.example.WebSideProject.service.WeatherMailHistoryService;
import com.example.WebSideProject.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/weather-mails")
@RequiredArgsConstructor
public class WeatherMailController {

    private final WeatherMailScheduler weatherMailScheduler;
    private final WeatherMailHistoryService weatherMailHistoryService;
    private final UserService userService;
    private final WeatherService weatherService;
    private final MailService mailService;

    @Value("${admin.api-key:}")
    private String adminApiKey;

    @Value("${admin.require-key:false}")
    private boolean adminRequireKey;

    @PostMapping("/send-now")
    public ResponseEntity<Map<String, String>> sendNow(
            @RequestParam(defaultValue = "MORNING") WeatherPeriod period,
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey
    ) {
        validateAdminKey(adminKey);
        weatherMailScheduler.sendWeatherMailByPeriod(period);
        return ResponseEntity.ok(Map.of("message", period.getLabel() + " 날씨 메일 발송을 시작했습니다."));
    }

    @PostMapping("/send-all")
    public ResponseEntity<Map<String, String>> sendAll(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey
    ) {
        validateAdminKey(adminKey);
        weatherMailScheduler.sendAllWeatherMails();
        return ResponseEntity.ok(Map.of("message", "아침/점심/저녁 날씨 메일 전체 발송을 시작했습니다."));
    }

    @PostMapping("/send-test")
    public ResponseEntity<Map<String, String>> sendTest(
            @RequestParam String email,
            @RequestParam(defaultValue = "MORNING") WeatherPeriod period,
            @RequestParam String baseDate,
            @RequestParam String baseTime,
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey
    ) {
        validateAdminKey(adminKey);
        User user = userService.getUserByEmail(email);
        WeatherDto weather = weatherService.getWeatherForBase(
                user.getNx(),
                user.getNy(),
                period,
                user.getLocationName(),
                baseDate,
                baseTime
        );
        mailService.sendWeatherMail(user, weather);
        return ResponseEntity.ok(Map.of("message", email + " 테스트 메일 발송을 시작했습니다."));
    }

    @GetMapping("/histories")
    public ResponseEntity<List<WeatherMailHistoryDto.Response>> getHistories(
            @RequestParam(required = false) String email,
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey
    ) {
        validateAdminKey(adminKey);
        return ResponseEntity.ok(weatherMailHistoryService.getRecentHistories(email));
    }

    private void validateAdminKey(String adminKey) {
        if (adminApiKey == null || adminApiKey.isBlank()) {
            if (adminRequireKey) {
                throw new SecurityException("관리자 API가 비활성화되어 있습니다.");
            }
            return;
        }
        if (adminKey == null || !MessageDigest.isEqual(
                adminApiKey.getBytes(StandardCharsets.UTF_8),
                adminKey.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new SecurityException("관리자 키가 올바르지 않습니다.");
        }
    }
}

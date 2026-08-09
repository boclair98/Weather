package com.example.WebSideProject.scheduler;

import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.dto.WeatherDto;
import com.example.WebSideProject.entity.User;
import com.example.WebSideProject.service.MailService;
import com.example.WebSideProject.service.UserService;
import com.example.WebSideProject.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmartWeatherAlertScheduler {

    private final WeatherService weatherService;
    private final UserService userService;
    private final MailService mailService;

    @Scheduled(cron = "0 15 7-21/2 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "smart-weather-alert", lockAtLeastFor = "PT1M", lockAtMostFor = "PT25M")
    public void sendSmartWeatherAlerts() {
        WeatherPeriod period = currentPeriod();
        List<User> users = userService.getSubscribedUsers().stream()
                .filter(User::isSmartAlertEnabled)
                .toList();
        log.info("스마트 날씨 알림 점검 시작: period={}, users={}", period, users.size());

        for (User user : users) {
            try {
                WeatherDto weather = weatherService.getWeather(
                        user.getNx(), user.getNy(), period, user.getLocationName());
                List<String> alerts = enabledAlerts(user, weather);
                if (alerts.isEmpty()) {
                    continue;
                }

                String alertSummary = String.join(" · ", alerts) + " 주의";
                String fingerprint = weather.getDate() + ":" + period.name() + ":" + String.join("-", alerts);
                if (user.hasReceivedSmartAlert(fingerprint)
                        || !userService.markSmartAlertSent(user.getId(), fingerprint)) {
                    continue;
                }
                mailService.sendSmartAlertMail(user, weather, alertSummary);
            } catch (Exception e) {
                log.error("스마트 날씨 알림 처리 실패: userId={}", user.getId(), e);
            }
        }
    }

    private List<String> enabledAlerts(User user, WeatherDto weather) {
        List<String> alerts = new ArrayList<>();
        if (user.isRainAlertEnabled() && weather.isRainRisk()) {
            alerts.add("비·우산");
        }
        if (user.isTemperatureAlertEnabled() && weather.isTemperatureRisk()) {
            alerts.add(parseTemperature(weather) >= 30 ? "폭염" : "한파");
        }
        if (user.isAirQualityAlertEnabled() && weather.isAirQualityRisk()) {
            alerts.add("대기질");
        }
        if (user.isWindAlertEnabled() && weather.isWindRisk()) {
            alerts.add("강풍");
        }
        return alerts;
    }

    private int parseTemperature(WeatherDto weather) {
        try {
            return Integer.parseInt(weather.getTmp());
        } catch (NumberFormatException ignored) {
            return 20;
        }
    }

    private WeatherPeriod currentPeriod() {
        int hour = LocalTime.now().getHour();
        if (hour < 11) return WeatherPeriod.MORNING;
        if (hour < 17) return WeatherPeriod.AFTERNOON;
        return WeatherPeriod.EVENING;
    }
}

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

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherMailScheduler {

    private final WeatherService weatherService;
    private final UserService userService;
    private final MailService mailService;

    @Scheduled(cron = "0 30 6 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "weather-mail-morning", lockAtLeastFor = "PT1M", lockAtMostFor = "PT30M")
    public void sendMorningWeatherMail() {
        sendWeatherMailByPeriod(WeatherPeriod.MORNING);
    }

    @Scheduled(cron = "0 30 11 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "weather-mail-afternoon", lockAtLeastFor = "PT1M", lockAtMostFor = "PT30M")
    public void sendAfternoonWeatherMail() {
        sendWeatherMailByPeriod(WeatherPeriod.AFTERNOON);
    }

    @Scheduled(cron = "0 30 18 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "weather-mail-evening", lockAtLeastFor = "PT1M", lockAtMostFor = "PT30M")
    public void sendEveningWeatherMail() {
        sendWeatherMailByPeriod(WeatherPeriod.EVENING);
    }

    public void sendDailyWeatherMail() {
        sendWeatherMailByPeriod(WeatherPeriod.MORNING);
    }

    public void sendAllWeatherMails() {
        for (WeatherPeriod period : WeatherPeriod.values()) {
            sendWeatherMailByPeriod(period);
        }
    }

    public void sendWeatherMailByPeriod(WeatherPeriod period) {
        log.info("=== {} 날씨 메일 발송 시작 ===", period.getLabel());

        List<User> subscribers = userService.getSubscribedUsers();
        log.info("구독자 수: {}명", subscribers.size());

        for (User user : subscribers) {
            if (!isEnabledForPeriod(user, period)) {
                continue;
            }

            try {
                WeatherDto weather = weatherService.getWeather(user.getNx(), user.getNy(), period, user.getLocationName());
                mailService.sendWeatherMail(user, weather);
            } catch (Exception e) {
                log.error("사용자 {} 처리 중 오류 발생", user.getEmail(), e);
            }
        }

        log.info("=== {} 날씨 메일 발송 완료 ===", period.getLabel());
    }

    private boolean isEnabledForPeriod(User user, WeatherPeriod period) {
        return switch (period) {
            case MORNING -> user.isMorningEnabled();
            case AFTERNOON -> user.isAfternoonEnabled();
            case EVENING -> user.isEveningEnabled();
        };
    }
}

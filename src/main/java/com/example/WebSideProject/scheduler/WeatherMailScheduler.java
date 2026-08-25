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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherMailScheduler {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final WeatherService weatherService;
    private final UserService userService;
    private final MailService mailService;

    /**
     * Checks every minute so each subscriber can choose an exact delivery time.
     * The repository query only loads users due in this minute, so this does not
     * fan out weather API calls for the entire subscriber base.
     */
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "weather-mail-custom-schedule", lockAtLeastFor = "PT1M", lockAtMostFor = "PT5M")
    public void sendScheduledWeatherMail() {
        LocalDateTime now = LocalDateTime.now(KOREA_ZONE).withSecond(0).withNano(0);
        for (WeatherPeriod period : WeatherPeriod.values()) {
            sendDueWeatherMailByPeriod(period, now);
        }
    }

    public void sendMorningWeatherMail() {
        sendWeatherMailByPeriod(WeatherPeriod.MORNING);
    }

    public void sendAfternoonWeatherMail() {
        sendWeatherMailByPeriod(WeatherPeriod.AFTERNOON);
    }

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
                log.error("사용자 메일 처리 중 오류 발생: userId={}", user.getId(), e);
            }
        }

        log.info("=== {} 날씨 메일 발송 완료 ===", period.getLabel());
    }

    private void sendDueWeatherMailByPeriod(WeatherPeriod period, LocalDateTime now) {
        List<User> subscribers = userService.getDueSubscribedUsers(period, now.toLocalTime());
        if (subscribers.isEmpty()) {
            return;
        }

        log.info("사용자 지정 {} 날씨 메일 발송 시작: dueUsers={}", period.getLabel(), subscribers.size());
        for (User user : subscribers) {
            try {
                WeatherDto weather = weatherService.getWeather(
                        user.getNx(), user.getNy(), period, user.getLocationName());
                if (!userService.claimScheduledMail(user.getId(), period, now.toLocalDate())) {
                    continue;
                }
                mailService.sendWeatherMail(user, weather);
            } catch (Exception e) {
                log.error("사용자 지정 메일 처리 중 오류 발생: userId={}, period={}", user.getId(), period, e);
            }
        }
    }

    private boolean isEnabledForPeriod(User user, WeatherPeriod period) {
        return switch (period) {
            case MORNING -> user.isMorningEnabled();
            case AFTERNOON -> user.isAfternoonEnabled();
            case EVENING -> user.isEveningEnabled();
        };
    }
}

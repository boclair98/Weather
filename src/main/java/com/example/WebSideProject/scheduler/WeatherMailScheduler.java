package com.example.WebSideProject.scheduler;

import com.example.WebSideProject.dto.WeatherDto;
import com.example.WebSideProject.entity.User;
import com.example.WebSideProject.service.MailService;
import com.example.WebSideProject.service.UserService;
import com.example.WebSideProject.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // 매일 오후 11시 40분 실행
    @Scheduled(cron = "0 40 23 * * *", zone = "Asia/Seoul")
    public void sendDailyWeatherMail() {
        log.info("=== 날씨 메일 발송 시작 ===");

        List<User> subscribers = userService.getSubscribedUsers();
        log.info("구독자 수: {}명", subscribers.size());

        for (User user : subscribers) {
            try {
                WeatherDto weather = weatherService.getWeather(user.getNx(), user.getNy());
                mailService.sendWeatherMail(user, weather);
            } catch (Exception e) {
                log.error("사용자 {} 처리 중 오류 발생", user.getEmail(), e);
            }
        }

        log.info("=== 날씨 메일 발송 완료: {}명 ===", subscribers.size());
    }
}

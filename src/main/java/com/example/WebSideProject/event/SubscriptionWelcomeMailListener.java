package com.example.WebSideProject.event;

import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.dto.WeatherDto;
import com.example.WebSideProject.entity.User;
import com.example.WebSideProject.service.MailService;
import com.example.WebSideProject.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionWelcomeMailListener {

    private final WeatherService weatherService;
    private final MailService mailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void sendWelcomeWeatherMail(SubscriptionWelcomeMailRequested event) {
        User user = event.user();
        try {
            WeatherDto weather = weatherService.getWeather(
                    user.getNx(),
                    user.getNy(),
                    WeatherPeriod.MORNING,
                    user.getLocationName()
            );
            mailService.sendWeatherMail(user, weather);
        } catch (Exception e) {
            log.error("구독 직후 날씨 메일 발송 준비 실패: {}", user.getEmail(), e);
        }
    }
}

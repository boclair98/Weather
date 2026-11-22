package com.example.WebSideProject.scheduler;

import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.service.MailService;
import com.example.WebSideProject.service.UserService;
import com.example.WebSideProject.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SmartWeatherAlertSchedulerTest {
    @Test
    void selectsAfternoonAndEveningByKoreanTimeWithAUtcClock() {
        SmartWeatherAlertScheduler scheduler = new SmartWeatherAlertScheduler(
                mock(WeatherService.class), mock(UserService.class), mock(MailService.class));
        ReflectionTestUtils.setField(scheduler, "clock",
                Clock.fixed(Instant.parse("2026-10-23T02:00:00Z"), ZoneOffset.UTC));
        WeatherPeriod afternoon = ReflectionTestUtils.invokeMethod(scheduler, "currentPeriod");
        assertThat(afternoon).isEqualTo(WeatherPeriod.AFTERNOON);

        ReflectionTestUtils.setField(scheduler, "clock",
                Clock.fixed(Instant.parse("2026-10-23T08:00:00Z"), ZoneOffset.UTC));
        WeatherPeriod evening = ReflectionTestUtils.invokeMethod(scheduler, "currentPeriod");
        assertThat(evening).isEqualTo(WeatherPeriod.EVENING);
    }
}

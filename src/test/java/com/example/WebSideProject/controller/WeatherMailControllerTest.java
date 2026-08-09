package com.example.WebSideProject.controller;

import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.dto.WeatherDto;
import com.example.WebSideProject.entity.User;
import com.example.WebSideProject.scheduler.WeatherMailScheduler;
import com.example.WebSideProject.service.MailService;
import com.example.WebSideProject.service.WeatherMailHistoryService;
import com.example.WebSideProject.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeatherMailControllerTest {

    @Test
    void sendsAdminTestMailForExplicitLocationWithoutSubscriptionRecord() {
        WeatherService weatherService = mock(WeatherService.class);
        MailService mailService = mock(MailService.class);
        WeatherDto weather = mock(WeatherDto.class);
        when(weatherService.getWeather(60, 127, WeatherPeriod.MORNING, "을지로3가"))
                .thenReturn(weather);
        WeatherMailController controller = new WeatherMailController(
                mock(WeatherMailScheduler.class),
                mock(WeatherMailHistoryService.class),
                weatherService,
                mailService
        );
        ReflectionTestUtils.setField(controller, "adminApiKey", "admin-secret");
        ReflectionTestUtils.setField(controller, "adminRequireKey", true);

        var response = controller.sendTest(
                "recipient@example.com", WeatherPeriod.MORNING,
                null, null, 60, 127, "을지로3가", "admin-secret"
        );

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(mailService).sendWeatherMail(user.capture(), org.mockito.ArgumentMatchers.same(weather));
        assertThat(user.getValue().getEmail()).isEqualTo("recipient@example.com");
        assertThat(user.getValue().getLocationName()).isEqualTo("을지로3가");
        assertThat(response.getBody()).containsEntry("locationName", "을지로3가");
    }
}

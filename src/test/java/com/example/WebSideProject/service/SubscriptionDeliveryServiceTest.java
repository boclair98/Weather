package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.UserDto;
import com.example.WebSideProject.repository.WeatherMailHistoryRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionDeliveryServiceTest {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Test
    void choosesNextEnabledTimeAcrossMidnight() {
        UserDto.Response subscription = UserDto.Response.builder()
                .morningEnabled(true).morningTime(LocalTime.of(7, 30))
                .eveningEnabled(true).eveningTime(LocalTime.of(18, 0))
                .build();

        ZonedDateTime now = ZonedDateTime.of(2026, 9, 30, 18, 1, 0, 0, KOREA_ZONE);

        assertThat(SubscriptionDeliveryService.nextScheduledAt(subscription, now))
                .isEqualTo(ZonedDateTime.of(2026, 10, 1, 7, 30, 0, 0, KOREA_ZONE));
    }

    @Test
    void readsOnlyTheAuthenticatedOwnersHistory() {
        UserService users = mock(UserService.class);
        WeatherMailHistoryRepository histories = mock(WeatherMailHistoryRepository.class);
        UserDto.Response subscription = UserDto.Response.builder()
                .email("owner@example.com").subscribed(true)
                .morningEnabled(true).morningTime(LocalTime.of(7, 30))
                .build();
        when(users.findCurrentSubscription("owner-id")).thenReturn(Optional.of(subscription));
        when(histories.findTop5ByUserEmailOrderBySentAtDesc("owner@example.com"))
                .thenReturn(List.of());

        var overview = new SubscriptionDeliveryService(users, histories).getOwnOverview("owner-id");

        assertThat(overview.nextScheduledAt()).isNotBlank();
        assertThat(overview.timezone()).isEqualTo("Asia/Seoul");
        verify(histories).findTop5ByUserEmailOrderBySentAtDesc("owner@example.com");
    }

    @Test
    void rejectsAnonymousDeliveryHistoryAccessBeforeQueryingStorage() {
        UserService users = mock(UserService.class);
        WeatherMailHistoryRepository histories = mock(WeatherMailHistoryRepository.class);

        assertThatThrownBy(() -> new SubscriptionDeliveryService(users, histories).getOwnOverview(null))
                .isInstanceOf(SecurityException.class);
        verify(histories, never()).findTop5ByUserEmailOrderBySentAtDesc(org.mockito.ArgumentMatchers.anyString());
    }
}

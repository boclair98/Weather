package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.DeliveryOverviewDto;
import com.example.WebSideProject.dto.UserDto;
import com.example.WebSideProject.entity.WeatherMailHistory;
import com.example.WebSideProject.repository.WeatherMailHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionDeliveryService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final UserService userService;
    private final WeatherMailHistoryRepository historyRepository;

    public DeliveryOverviewDto getOwnOverview(String codersUserId) {
        if (codersUserId == null || codersUserId.isBlank()) {
            throw new SecurityException("로그인 후 내 발송 현황을 확인해주세요.");
        }
        UserDto.Response subscription = userService.findCurrentSubscription(codersUserId)
                .orElseThrow(() -> new IllegalArgumentException("현재 계정에 연결된 구독이 없습니다."));
        ZonedDateTime now = ZonedDateTime.now(KOREA_ZONE);
        String nextScheduledAt = subscription.isSubscribed()
                ? nextScheduledAt(subscription, now).toOffsetDateTime().toString()
                : null;
        List<DeliveryOverviewDto.DeliveryAttempt> recent = historyRepository
                .findTop5ByUserEmailOrderBySentAtDesc(subscription.getEmail()).stream()
                .map(this::toAttempt)
                .toList();
        return new DeliveryOverviewDto(subscription.isSubscribed(), nextScheduledAt, KOREA_ZONE.getId(), recent);
    }

    static ZonedDateTime nextScheduledAt(UserDto.Response subscription, ZonedDateTime now) {
        List<ZonedDateTime> candidates = new ArrayList<>(3);
        addCandidate(candidates, subscription.isMorningEnabled(), subscription.getMorningTime(), now);
        addCandidate(candidates, subscription.isAfternoonEnabled(), subscription.getAfternoonTime(), now);
        addCandidate(candidates, subscription.isEveningEnabled(), subscription.getEveningTime(), now);
        return candidates.stream().min(Comparator.naturalOrder())
                .orElseThrow(() -> new IllegalStateException("설정된 알림 시간이 없습니다."));
    }

    private static void addCandidate(List<ZonedDateTime> candidates, boolean enabled, LocalTime time, ZonedDateTime now) {
        if (!enabled || time == null) return;
        ZonedDateTime candidate = now.toLocalDate().atTime(time).atZone(KOREA_ZONE);
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(1);
        candidates.add(candidate);
    }

    private DeliveryOverviewDto.DeliveryAttempt toAttempt(WeatherMailHistory history) {
        return new DeliveryOverviewDto.DeliveryAttempt(
                history.getSentAt().atZone(KOREA_ZONE).toOffsetDateTime().toString(),
                history.getStatus(),
                history.getLocationName()
        );
    }
}

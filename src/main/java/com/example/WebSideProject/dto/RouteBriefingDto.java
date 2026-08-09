package com.example.WebSideProject.dto;

import java.util.ArrayList;
import java.util.List;

public record RouteBriefingDto(
        LocationDto.Response origin,
        LocationDto.Response destination,
        int distanceMeters,
        int durationMinutes,
        String distanceLabel,
        String durationLabel,
        String headline,
        List<String> checklist,
        WeatherDto originWeather,
        WeatherDto destinationWeather
) {
    public static RouteBriefingDto from(
            LocationDto.Response origin,
            LocationDto.Response destination,
            int distanceMeters,
            int durationSeconds,
            WeatherDto originWeather,
            WeatherDto destinationWeather
    ) {
        int durationMinutes = Math.max(1, (int) Math.ceil(durationSeconds / 60.0));
        List<String> checklist = new ArrayList<>();
        if (originWeather.isRainRisk() || destinationWeather.isRainRisk()) {
            checklist.add("출발지 또는 목적지에 비 가능성이 있어 우산을 챙기세요.");
        }
        if (destinationWeather.isAirQualityRisk()) {
            checklist.add("목적지 대기질이 좋지 않아 마스크를 권장해요.");
        }
        if (destinationWeather.isUvRisk()) {
            checklist.add("도착 후 자외선이 강해 선크림과 모자를 준비하세요.");
        }
        if (destinationWeather.isPollenRisk()) {
            checklist.add("목적지 꽃가루 위험이 높아 민감군은 마스크를 준비하세요.");
        }
        if (destinationWeather.isOfficialWarningRisk()) {
            checklist.add("목적지에 " + destinationWeather.getWeatherWarningTitle() + "가 발효 중입니다.");
        }

        int temperatureGap = Math.abs(parseTemperature(originWeather) - parseTemperature(destinationWeather));
        if (temperatureGap >= 4) {
            checklist.add("출발지와 목적지의 기온 차가 " + temperatureGap + "°C라 벗기 쉬운 겉옷이 좋아요.");
        }
        if (checklist.isEmpty()) {
            checklist.add("경로 전반에 큰 날씨 변수는 적어 보여요.");
        }

        String headline = destinationWeather.getOutingScore() >= 70
                ? "이동하기 무난한 경로예요. 도착지 준비만 확인하세요."
                : "도착지 날씨 변수가 있어 출발 전 준비가 필요해요.";
        return new RouteBriefingDto(
                origin,
                destination,
                distanceMeters,
                durationMinutes,
                formatDistance(distanceMeters),
                formatDuration(durationMinutes),
                headline,
                List.copyOf(checklist),
                originWeather,
                destinationWeather
        );
    }

    private static String formatDistance(int meters) {
        if (meters < 1000) return meters + "m";
        return String.format("%.1fkm", meters / 1000.0);
    }

    private static String formatDuration(int minutes) {
        if (minutes < 60) return minutes + "분";
        return (minutes / 60) + "시간 " + (minutes % 60) + "분";
    }

    private static int parseTemperature(WeatherDto weather) {
        try {
            return Integer.parseInt(weather.getTmp());
        } catch (NumberFormatException ignored) {
            return 20;
        }
    }
}

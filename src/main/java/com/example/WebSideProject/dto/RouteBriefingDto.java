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

        Integer originTemperature = parseTemperature(originWeather);
        Integer destinationTemperature = parseTemperature(destinationWeather);
        if (originTemperature != null && destinationTemperature != null) {
            int temperatureGap = Math.abs(originTemperature - destinationTemperature);
            if (temperatureGap >= 4) {
                checklist.add("확인된 출발지와 목적지의 기온 차가 " + temperatureGap + "°C라 벗기 쉬운 겉옷이 좋아요.");
            }
        } else {
            checklist.add("일부 지역의 기온 자료가 없어 기온 차를 비교하지 못했어요. 이동 전 최신 기온을 확인하세요.");
        }
        if (!originWeather.hasPrecipitationData() || !destinationWeather.hasPrecipitationData()) {
            checklist.add("일부 지역의 강수 자료가 부족해요. 이동 전 비나 눈 예보를 다시 확인하세요.");
        }
        boolean complete = originWeather.hasCoreWeatherData() && destinationWeather.hasCoreWeatherData();
        if (checklist.isEmpty()) {
            checklist.add(complete ? "확인된 예보에서는 큰 날씨 변수가 적어요. 출발 전 최신 예보도 확인하세요."
                    : "일부 날씨 자료가 없어 판단하기 어려워요. 출발 전 최신 예보를 확인하세요.");
        }

        String headline = !complete ? "일부 날씨 자료가 부족해요. 이동 전 최신 예보를 확인하세요."
                : destinationWeather.getOutingScore() >= 70
                    ? "확인된 예보에서는 이동이 무난해요. 출발지와 도착지 예보를 확인하세요."
                    : "확인된 도착지 예보에 날씨 변수가 있어 출발 전 준비가 필요해요.";
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

    private static Integer parseTemperature(WeatherDto weather) {
        return weather.hasTemperatureData() ? Integer.parseInt(weather.getTmp()) : null;
    }
}

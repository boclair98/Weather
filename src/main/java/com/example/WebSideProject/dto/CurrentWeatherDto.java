package com.example.WebSideProject.dto;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public record CurrentWeatherDto(
        String locationName,
        String observedAt,
        String fetchedAt,
        double temperature,
        int apparentTemperature,
        int humidity,
        double windSpeed,
        String oneHourPrecipitation,
        String precipitationType,
        String headline,
        String advice,
        String sourceName,
        String sourceUrl,
        boolean fallback
) {
    public static CurrentWeatherDto of(
            String locationName,
            String observedAt,
            double temperature,
            int humidity,
            double windSpeed,
            String oneHourPrecipitation,
            String precipitationType
    ) {
        int apparentTemperature = calculateApparentTemperature(temperature, humidity, windSpeed);
        boolean precipitation = !"없음".equals(precipitationType);
        String headline = precipitation
                ? "지금 " + precipitationType + " · 체감 " + apparentTemperature + "°C"
                : "지금 " + formatTemperature(temperature) + "°C · 체감 " + apparentTemperature + "°C";
        String advice = buildAdvice(apparentTemperature, humidity, windSpeed, precipitationType);
        return new CurrentWeatherDto(
                locationName == null || locationName.isBlank() ? "선택 위치" : locationName,
                observedAt,
                OffsetDateTime.now(ZoneId.of("Asia/Seoul")).toString(),
                temperature,
                apparentTemperature,
                humidity,
                Math.round(windSpeed * 10.0) / 10.0,
                oneHourPrecipitation == null || oneHourPrecipitation.isBlank() ? "0" : oneHourPrecipitation,
                precipitationType,
                headline,
                advice,
                "기상청 초단기실황",
                "https://www.weather.go.kr/w/weather/forecast/short-term.do",
                false
        );
    }

    public CurrentWeatherDto asFallback() {
        return new CurrentWeatherDto(
                locationName, observedAt, fetchedAt, temperature, apparentTemperature, humidity,
                windSpeed, oneHourPrecipitation, precipitationType, headline,
                "원천 연결이 지연되어 마지막 정상 실황입니다. " + advice,
                sourceName, sourceUrl, true
        );
    }

    static int calculateApparentTemperature(double temperature, int humidity, double windSpeed) {
        double vaporPressure = (Math.max(0, Math.min(100, humidity)) / 100.0)
                * 6.105 * Math.exp((17.27 * temperature) / (237.7 + temperature));
        double apparent = temperature + (0.33 * vaporPressure) - (0.70 * Math.max(0, windSpeed)) - 4.0;
        return (int) Math.round(apparent);
    }

    private static String buildAdvice(
            int apparentTemperature,
            int humidity,
            double windSpeed,
            String precipitationType
    ) {
        if (!"없음".equals(precipitationType)) {
            return "관측상 강수가 확인됐어요. 우산을 챙기고 미끄러운 길을 주의하세요.";
        }
        if (apparentTemperature >= 31) {
            return "현재 체감이 매우 더워요. 물을 챙기고 장시간 야외 활동을 줄이세요.";
        }
        if (apparentTemperature <= 0) {
            return "현재 체감이 영하권이에요. 장갑과 보온 겉옷을 챙기세요.";
        }
        if (windSpeed >= 8.0) {
            return "현재 바람이 강해요. 가벼운 물건과 우산 사용에 주의하세요.";
        }
        if (humidity >= 85) {
            return "현재 습도가 높아요. 통풍이 잘되는 옷차림이 편합니다.";
        }
        return "현재 관측상 큰 불편 신호는 없어요. 아래 시간별 예보도 함께 확인하세요.";
    }

    private static String formatTemperature(double value) {
        return value == Math.rint(value) ? Integer.toString((int) value) : Double.toString(value);
    }
}

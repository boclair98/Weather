package com.example.WebSideProject.dto;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record ForecastProvenanceDto(
        String sourceName,
        String sourceUrl,
        String forecastIssuedAt,
        String fetchedAt,
        long ageSeconds,
        String freshness,
        String qualityStatus,
        int completenessPercent,
        boolean fallback,
        String notice
) {
    private static final int EXPECTED_FIELDS_PER_PERIOD = 6;

    public static ForecastProvenanceDto from(List<DailyWeatherDto> forecasts) {
        List<WeatherDto> periods = forecasts.stream()
                .flatMap(daily -> List.of(daily.morning(), daily.afternoon(), daily.evening()).stream())
                .toList();
        WeatherDto representative = periods.get(0);
        int available = periods.stream().mapToInt(ForecastProvenanceDto::availableFields).sum();
        int expected = periods.size() * EXPECTED_FIELDS_PER_PERIOD;
        int completeness = expected == 0 ? 0 : (int) Math.round(available * 100.0 / expected);
        Instant fetchedAt = parseInstant(representative.getDataFetchedAt());
        long ageSeconds = Math.max(0, Duration.between(fetchedAt, Instant.now()).getSeconds());
        boolean fallback = representative.isFallbackData();
        String freshness = fallback ? "STALE_FALLBACK"
                : ageSeconds <= 900 ? "FRESH"
                : ageSeconds <= 3600 ? "RECENT" : "STALE";
        String quality = fallback ? "DEGRADED"
                : completeness >= 90 ? "VERIFIED"
                : completeness >= 70 ? "PARTIAL" : "DEGRADED";

        return new ForecastProvenanceDto(
                representative.getDataSourceName(),
                representative.getDataSourceUrl(),
                representative.getForecastIssuedAt(),
                representative.getDataFetchedAt(),
                ageSeconds,
                freshness,
                quality,
                completeness,
                fallback,
                fallback
                        ? "외부 원천 장애로 마지막 정상 예보를 제공합니다. 발표시각을 확인하세요."
                        : "기상청 원천자료를 서비스 목적에 맞게 재구성한 정보입니다."
        );
    }

    private static int availableFields(WeatherDto weather) {
        if (weather.getSourceFieldCount() >= 0) {
            return weather.getSourceFieldCount();
        }
        return (present(weather.getTmp()) ? 1 : 0)
                + (present(weather.getPop()) ? 1 : 0)
                + (present(weather.getReh()) ? 1 : 0)
                + (present(weather.getWsd()) ? 1 : 0)
                + (!"알 수 없음".equals(weather.getSkyDescription()) ? 1 : 0)
                + (!"알 수 없음".equals(weather.getPtyDescription()) ? 1 : 0);
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank() && !"-".equals(value);
    }

    private static Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return Instant.EPOCH;
        }
    }
}

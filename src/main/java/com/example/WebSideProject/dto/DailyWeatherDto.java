package com.example.WebSideProject.dto;

import com.example.WebSideProject.Enum.AgeGroup;
import com.example.WebSideProject.Enum.ActivityType;
import com.example.WebSideProject.Enum.GenderType;
import com.example.WebSideProject.Enum.TemperatureSensitivity;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public record DailyWeatherDto(
        WeatherDto morning,
        WeatherDto afternoon,
        WeatherDto evening,
        String bestPeriod,
        String bestPeriodLabel,
        int bestOutingScore,
        String headline,
        String temperatureTrend,
        String rainSummary,
        String dayLabel,
        String forecastDate
) {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    public static DailyWeatherDto from(WeatherDto morning, WeatherDto afternoon, WeatherDto evening) {
        return from(morning, afternoon, evening, "오늘", morning.getDate());
    }

    public static DailyWeatherDto from(
            WeatherDto morning,
            WeatherDto afternoon,
            WeatherDto evening,
            String dayLabel,
            String forecastDate
    ) {
        return from(morning, afternoon, evening, dayLabel, forecastDate, Clock.systemUTC());
    }

    public static DailyWeatherDto from(
            WeatherDto morning,
            WeatherDto afternoon,
            WeatherDto evening,
            String dayLabel,
            String forecastDate,
            Clock clock
    ) {
        int currentHour = "오늘".equals(dayLabel)
                ? LocalTime.now(clock.withZone(KOREA_ZONE)).getHour() : 0;
        return fromAtHour(morning, afternoon, evening, currentHour, dayLabel, forecastDate);
    }

    static DailyWeatherDto fromAtHour(
            WeatherDto morning,
            WeatherDto afternoon,
            WeatherDto evening,
            int currentHour
    ) {
        return fromAtHour(morning, afternoon, evening, currentHour, "오늘", morning.getDate());
    }

    static DailyWeatherDto fromAtHour(
            WeatherDto morning,
            WeatherDto afternoon,
            WeatherDto evening,
            int currentHour,
            String dayLabel,
            String forecastDate
    ) {
        List<PeriodWeather> periods = List.of(
                new PeriodWeather("MORNING", "아침", 9, morning),
                new PeriodWeather("AFTERNOON", "점심", 12, afternoon),
                new PeriodWeather("EVENING", "저녁", 21, evening)
        );
        List<PeriodWeather> remainingPeriods = periods.stream()
                .filter(period -> period.hour() >= currentHour)
                .toList();
        List<PeriodWeather> recommendationPool = remainingPeriods.isEmpty() ? periods : remainingPeriods;
        PeriodWeather best = recommendationPool.stream()
                .filter(period -> period.weather().hasCoreWeatherData())
                .max((left, right) -> Integer.compare(
                        left.weather().getOutingScore(),
                        right.weather().getOutingScore()
                ))
                .orElse(null);

        return new DailyWeatherDto(
                morning,
                afternoon,
                evening,
                best == null ? "UNAVAILABLE" : best.code(),
                best == null ? "자료 부족" : best.label(),
                best == null ? -1 : best.weather().getOutingScore(),
                buildHeadline(best, periods, dayLabel),
                buildTemperatureTrend(morning, afternoon, evening),
                buildRainSummary(periods),
                dayLabel,
                forecastDate
        );
    }

    public DailyWeatherDto withStylePreference(AgeGroup ageGroup, GenderType gender) {
        return withStylePreference(ageGroup, gender, TemperatureSensitivity.NONE, ActivityType.DAILY);
    }

    public DailyWeatherDto withStylePreference(
            AgeGroup ageGroup,
            GenderType gender,
            TemperatureSensitivity temperatureSensitivity,
            ActivityType activityType
    ) {
        return new DailyWeatherDto(
                morning.withStylePreference(ageGroup, gender, temperatureSensitivity, activityType),
                afternoon.withStylePreference(ageGroup, gender, temperatureSensitivity, activityType),
                evening.withStylePreference(ageGroup, gender, temperatureSensitivity, activityType),
                bestPeriod,
                bestPeriodLabel,
                bestOutingScore,
                headline,
                temperatureTrend,
                rainSummary,
                dayLabel,
                forecastDate
        );
    }

    private static String buildHeadline(PeriodWeather best, List<PeriodWeather> periods, String dayLabel) {
        if (best == null) return dayLabel + "은 일부 예보가 없어 시간대 비교를 준비하지 못했어요.";
        if (periods.stream().anyMatch(period -> !period.weather().hasCoreWeatherData())) {
            return dayLabel + " 확인된 예보 중에는 " + best.label() + "을 참고해 주세요.";
        }
        long difficultPeriods = periods.stream()
                .filter(period -> period.weather().hasCoreWeatherData()
                        && period.weather().getOutingScore() < 70)
                .count();
        if (difficultPeriods == periods.size()) {
            return dayLabel + "은 세 시간대 모두 준비가 필요해요.";
        }
        return dayLabel + " " + best.label() + "이 가장 외출하기 좋아요.";
    }

    private static String buildTemperatureTrend(
            WeatherDto morning,
            WeatherDto afternoon,
            WeatherDto evening
    ) {
        return "아침 " + morning.getTmp() + "°C → 점심 " + afternoon.getTmp()
                + "°C → 저녁 " + evening.getTmp() + "°C";
    }

    private static String buildRainSummary(List<PeriodWeather> periods) {
        List<String> rainyPeriods = periods.stream()
                .filter(period -> popChance(period.weather()) >= 40
                        || hasPrecipitation(period.weather()))
                .map(PeriodWeather::label)
                .toList();

        if (rainyPeriods.isEmpty()) {
            if (periods.stream().anyMatch(period -> !period.weather().hasPrecipitationData())) {
                return "일부 시간대의 강수 자료가 없어요. 외출 전 비 예보를 다시 확인하세요.";
            }
            return "세 시간대 모두 우산 부담이 낮아요.";
        }
        return String.join("·", rainyPeriods) + " 외출에는 우산을 확인하세요.";
    }

    private static int popChance(WeatherDto weather) {
        try {
            return Integer.parseInt(weather.getPop());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static boolean hasPrecipitation(WeatherDto weather) {
        return List.of("1", "2", "3", "4").contains(weather.getPty() == null ? "" : weather.getPty());
    }

    private record PeriodWeather(String code, String label, int hour, WeatherDto weather) {
    }
}

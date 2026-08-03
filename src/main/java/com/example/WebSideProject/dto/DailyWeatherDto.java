package com.example.WebSideProject.dto;

import com.example.WebSideProject.Enum.AgeGroup;
import com.example.WebSideProject.Enum.GenderType;

import java.time.LocalTime;
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
        String rainSummary
) {

    public static DailyWeatherDto from(WeatherDto morning, WeatherDto afternoon, WeatherDto evening) {
        return fromAtHour(morning, afternoon, evening, LocalTime.now().getHour());
    }

    static DailyWeatherDto fromAtHour(
            WeatherDto morning,
            WeatherDto afternoon,
            WeatherDto evening,
            int currentHour
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
                .max((left, right) -> Integer.compare(
                        left.weather().getOutingScore(),
                        right.weather().getOutingScore()
                ))
                .orElse(recommendationPool.get(0));

        return new DailyWeatherDto(
                morning,
                afternoon,
                evening,
                best.code(),
                best.label(),
                best.weather().getOutingScore(),
                buildHeadline(best, periods),
                buildTemperatureTrend(morning, afternoon, evening),
                buildRainSummary(periods)
        );
    }

    public DailyWeatherDto withStylePreference(AgeGroup ageGroup, GenderType gender) {
        return new DailyWeatherDto(
                morning.withStylePreference(ageGroup, gender),
                afternoon.withStylePreference(ageGroup, gender),
                evening.withStylePreference(ageGroup, gender),
                bestPeriod,
                bestPeriodLabel,
                bestOutingScore,
                headline,
                temperatureTrend,
                rainSummary
        );
    }

    private static String buildHeadline(PeriodWeather best, List<PeriodWeather> periods) {
        long difficultPeriods = periods.stream()
                .filter(period -> period.weather().getOutingScore() < 70)
                .count();
        if (difficultPeriods == periods.size()) {
            return "오늘은 세 시간대 모두 준비가 필요해요.";
        }
        return best.label() + "이 가장 외출하기 좋아요.";
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
        String description = weather.getPtyDescription();
        return !"없음".equals(description) && !"알 수 없음".equals(description);
    }

    private record PeriodWeather(String code, String label, int hour, WeatherDto weather) {
    }
}

package com.example.WebSideProject.dto;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class DailyWeatherDtoTest {

    @Test
    void recommendsBestOutingPeriodAndSummarizesTheDay() {
        WeatherDto morning = weather("18", "40", "0", "1");
        WeatherDto afternoon = weather("25", "10", "0", "1");
        WeatherDto evening = weather("21", "80", "1", "4");

        DailyWeatherDto daily = DailyWeatherDto.fromAtHour(morning, afternoon, evening, 8);

        assertThat(daily.bestPeriod()).isEqualTo("AFTERNOON");
        assertThat(daily.bestPeriodLabel()).isEqualTo("점심");
        assertThat(daily.headline()).contains("점심");
        assertThat(daily.temperatureTrend()).isEqualTo("아침 18°C → 점심 25°C → 저녁 21°C");
        assertThat(daily.rainSummary()).contains("저녁").contains("우산");
    }

    @Test
    void reportsLowUmbrellaBurdenWhenAllPeriodsAreDry() {
        DailyWeatherDto daily = DailyWeatherDto.fromAtHour(
                weather("16", "10", "0", "1"),
                weather("23", "20", "0", "1"),
                weather("19", "10", "0", "3"),
                8
        );

        assertThat(daily.rainSummary()).contains("우산 부담이 낮아요");
    }

    @Test
    void recommendsOnlyAWeatherPeriodThatHasNotPassed() {
        DailyWeatherDto daily = DailyWeatherDto.fromAtHour(
                weather("20", "0", "0", "1"),
                weather("24", "0", "0", "1"),
                weather("18", "30", "0", "3"),
                18
        );

        assertThat(daily.bestPeriod()).isEqualTo("EVENING");
    }

    @Test
    void usesKoreanCurrentHourForTodayWhileKeepingAllTomorrowPeriodsAvailable() {
        Clock utcClock = Clock.fixed(Instant.parse("2026-10-23T09:00:00Z"), ZoneOffset.UTC);
        WeatherDto morning = weather("20", "0", "0", "1");
        WeatherDto afternoon = weather("24", "10", "0", "1");
        WeatherDto evening = weather("18", "70", "1", "4");

        DailyWeatherDto today = DailyWeatherDto.from(
                morning, afternoon, evening, "오늘", "20261023", utcClock);
        DailyWeatherDto tomorrow = DailyWeatherDto.from(
                morning, afternoon, evening, "내일", "20261024", utcClock);

        assertThat(today.bestPeriod()).isEqualTo("EVENING");
        assertThat(tomorrow.bestPeriod()).isEqualTo("MORNING");
    }

    @Test
    void labelsTomorrowForecastWithoutFilteringPassedPeriods() {
        DailyWeatherDto daily = DailyWeatherDto.fromAtHour(
                weather("20", "0", "0", "1"),
                weather("24", "0", "0", "1"),
                weather("18", "20", "0", "3"),
                0,
                "내일",
                "20260804"
        );

        assertThat(daily.dayLabel()).isEqualTo("내일");
        assertThat(daily.forecastDate()).isEqualTo("20260804");
        assertThat(daily.headline()).startsWith("내일");
    }

    @Test
    void incompleteWeatherCannotBecomeTheRecommendedTimeOrADryDay() {
        WeatherDto missing = WeatherDto.builder().tmp("20").build();

        DailyWeatherDto daily = DailyWeatherDto.fromAtHour(missing, missing, missing, 8);

        assertThat(daily.bestPeriod()).isEqualTo("UNAVAILABLE");
        assertThat(daily.bestPeriodLabel()).isEqualTo("자료 부족");
        assertThat(daily.bestOutingScore()).isEqualTo(-1);
        assertThat(daily.headline()).contains("일부 예보가 없어");
        assertThat(daily.rainSummary()).contains("강수 자료가 없어요");
    }

    @Test
    void recommendsACompletePeriodInsteadOfAnIncompleteSunnyLookingPeriod() {
        WeatherDto missing = WeatherDto.builder().tmp("24").sky("1").build();
        WeatherDto complete = weather("19", "80", "1", "4");

        DailyWeatherDto daily = DailyWeatherDto.fromAtHour(missing, complete, missing, 8);

        assertThat(daily.bestPeriod()).isEqualTo("AFTERNOON");
        assertThat(daily.bestOutingScore()).isEqualTo(complete.getOutingScore());
        assertThat(daily.rainSummary()).contains("점심").contains("우산");
    }

    private WeatherDto weather(String temperature, String pop, String pty, String sky) {
        return WeatherDto.builder()
                .date("20260803")
                .time("0900")
                .periodLabel("예보")
                .tmp(temperature)
                .pop(pop)
                .pty(pty)
                .sky(sky)
                .reh("50")
                .wsd("2.0")
                .build();
    }
}

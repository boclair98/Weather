package com.example.WebSideProject.dto;

import org.junit.jupiter.api.Test;

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

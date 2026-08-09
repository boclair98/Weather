package com.example.WebSideProject.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherPlannerDtoTest {

    @Test
    void recommendsBestDayAndBuildsPackingChecklist() {
        DailyWeatherDto today = daily("오늘", "20260809",
                weather("24", "80", "1", "4"),
                weather("26", "70", "1", "4"),
                weather("22", "60", "0", "4"));
        DailyWeatherDto tomorrow = daily("내일", "20260810",
                weather("22", "10", "0", "1"),
                weather("25", "10", "0", "1"),
                weather("21", "10", "0", "1"));

        WeatherPlannerDto planner = WeatherPlannerDto.from("서울시청", List.of(today, tomorrow));

        assertThat(planner.recommendedDayLabel()).isEqualTo("내일");
        assertThat(planner.headline()).contains("내일");
        assertThat(planner.days()).hasSize(2);
        assertThat(planner.packingChecklist()).contains("접이식 우산");
        assertThat(planner.days().get(0).riskLevel()).isIn("MEDIUM", "HIGH");
    }

    @Test
    void rejectsEmptyForecasts() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> WeatherPlannerDto.from("서울", List.of())
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void turnsHotWeatherIntoHydrationAdvice() {
        DailyWeatherDto hotDay = daily("오늘", "20260809",
                weather("27", "0", "0", "1"),
                weather("32", "0", "0", "1"),
                weather("29", "0", "0", "1"));

        WeatherPlannerDto planner = WeatherPlannerDto.from("서울", List.of(hotDay));

        assertThat(planner.days().get(0).keyAction()).contains("물");
        assertThat(planner.packingChecklist()).contains("물·체온 조절 용품");
    }

    private DailyWeatherDto daily(
            String label,
            String date,
            WeatherDto morning,
            WeatherDto afternoon,
            WeatherDto evening
    ) {
        return DailyWeatherDto.fromAtHour(morning, afternoon, evening, 0, label, date);
    }

    private WeatherDto weather(String temperature, String pop, String pty, String sky) {
        return WeatherDto.builder()
                .date("20260809")
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

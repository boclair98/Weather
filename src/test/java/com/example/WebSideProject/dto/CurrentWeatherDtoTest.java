package com.example.WebSideProject.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentWeatherDtoTest {

    @Test
    void summarizesObservedRainAndApparentTemperature() {
        CurrentWeatherDto weather = CurrentWeatherDto.of(
                "강남역", "2026-08-13T14:00+09:00", 30.0, 80, 2.0, "1.2", "비"
        );

        assertThat(weather.headline()).contains("지금 비").contains("체감");
        assertThat(weather.advice()).contains("우산");
        assertThat(weather.sourceName()).isEqualTo("기상청 초단기실황");
        assertThat(weather.fallback()).isFalse();
    }

    @Test
    void calculatesAColderApparentTemperatureWhenWindIsStrong() {
        int calm = CurrentWeatherDto.calculateApparentTemperature(8.0, 55, 0.0);
        int windy = CurrentWeatherDto.calculateApparentTemperature(8.0, 55, 9.0);

        assertThat(windy).isLessThan(calm);
    }

    @Test
    void fallbackIsClearlyMarkedWithoutChangingObservation() {
        CurrentWeatherDto fallback = CurrentWeatherDto.of(
                "서울", "2026-08-13T14:00+09:00", 24.0, 50, 1.0, "0", "없음"
        ).asFallback();

        assertThat(fallback.fallback()).isTrue();
        assertThat(fallback.advice()).startsWith("원천 연결이 지연되어 마지막 정상 실황입니다.");
        assertThat(fallback.temperature()).isEqualTo(24.0);
    }
}

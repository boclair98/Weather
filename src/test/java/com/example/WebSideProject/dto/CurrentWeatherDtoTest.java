package com.example.WebSideProject.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentWeatherDtoTest {

    @Test
    void summarizesObservedRainAndApparentTemperature() {
        CurrentWeatherDto weather = CurrentWeatherDto.of(
                "강남역", "2026-08-13T14:00+09:00", 30.0, 80, 2.0, "1.2", "비"
        );

        assertThat(weather.headline()).isEqualTo("비 관측 중").doesNotContain("30", "체감");
        assertThat(weather.advice()).contains("우산");
        assertThat(weather.sourceName()).isEqualTo("기상청 초단기실황");
        assertThat(weather.fallback()).isFalse();
    }

    @Test
    void keepsTheCurrentWeatherHeadlineSeparateFromTemperatureAndFeelsLike() {
        CurrentWeatherDto weather = CurrentWeatherDto.of(
                "을지로3가역", "2026-08-13T14:00+09:00", 22.8, 33, 1.3, "0", "없음"
        );

        assertThat(weather.headline()).isEqualTo("강수 현상 없음");
        assertThat(weather.headline()).doesNotContain("22.8", "체감", "°C");
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

package com.example.WebSideProject.dto;

import org.junit.jupiter.api.Test;

import com.example.WebSideProject.Enum.AgeGroup;
import com.example.WebSideProject.Enum.GenderType;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherDtoTest {

    @Test
    void forecastLabelIncludesSelectedPeriod() {
        WeatherDto weather = WeatherDto.builder()
                .date("20260606")
                .time("2100")
                .periodLabel("저녁")
                .build();

        assertThat(weather.getForecastLabel()).isEqualTo("06월 06일 저녁 예보");
    }

    @Test
    void rainyAndBadAirQualityReduceOutingScore() {
        WeatherDto weather = WeatherDto.builder()
                .sky("4")
                .pty("1")
                .tmp("30")
                .pop("80")
                .reh("90")
                .wsd("9.0")
                .pm10Grade("3")
                .pm25Grade("2")
                .build();

        assertThat(weather.getOutingScore()).isLessThan(60);
        assertThat(weather.getUmbrellaAdvice()).contains("추천");
        assertThat(weather.getMaskAdvice()).contains("마스크");
    }

    @Test
    void styleAdviceChangesByWeatherContext() {
        WeatherDto rainy = WeatherDto.builder()
                .sky("4")
                .pty("1")
                .tmp("23")
                .pop("80")
                .wsd("3.2")
                .build();

        WeatherDto hotSunny = WeatherDto.builder()
                .sky("1")
                .pty("0")
                .tmp("32")
                .pop("10")
                .reh("82")
                .wsd("1.2")
                .build();

        assertThat(rainy.getFootwearAdvice()).contains("방수");
        assertThat(rainy.getStyleCaution()).contains("물 얼룩");
        assertThat(hotSunny.getTopAdvice()).contains("통풍");
        assertThat(hotSunny.getStyleRecommendation(AgeGroup.TWENTIES, GenderType.FEMALE))
                .contains("20대 여성")
                .contains("통풍");
        assertThat(rainy.getColorPalette()).isNotEqualTo(hotSunny.getColorPalette());
    }
}

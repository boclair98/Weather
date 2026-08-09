package com.example.WebSideProject.dto;

import org.junit.jupiter.api.Test;

import com.example.WebSideProject.Enum.AgeGroup;
import com.example.WebSideProject.Enum.ActivityType;
import com.example.WebSideProject.Enum.GenderType;
import com.example.WebSideProject.Enum.TemperatureSensitivity;

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
        assertThat(hotSunny.getStyleRecommendation(
                AgeGroup.NONE,
                GenderType.NONE,
                TemperatureSensitivity.HEAT,
                ActivityType.OUTDOOR))
                .contains("산책·야외 활동")
                .contains("통풍");
        assertThat(hotSunny.withStylePreference(
                        AgeGroup.NONE,
                        GenderType.NONE,
                        TemperatureSensitivity.HEAT,
                        ActivityType.OUTDOOR)
                .getTopAdvice()).contains("흡습·속건");
        assertThat(rainy.getColorPalette()).isNotEqualTo(hotSunny.getColorPalette());
    }

    @Test
    void smartAlertSummaryCombinesOnlyDetectedRisks() {
        WeatherDto risky = WeatherDto.builder()
                .date("20260809")
                .tmp("33")
                .pop("80")
                .pty("1")
                .wsd("9.2")
                .pm10Grade("3")
                .pm25Grade("2")
                .build();

        assertThat(risky.isRainRisk()).isTrue();
        assertThat(risky.isTemperatureRisk()).isTrue();
        assertThat(risky.isWindRisk()).isTrue();
        assertThat(risky.isAirQualityRisk()).isTrue();
        assertThat(risky.getSmartAlertSummary())
                .contains("비·우산", "폭염", "대기질", "강풍");
    }

    @Test
    void safetySignalsChangeScoreAndAdvice() {
        WeatherDto safe = WeatherDto.builder()
                .tmp("24")
                .pop("10")
                .uvIndex(2)
                .pollenType("잡초류")
                .pollenRiskLevel(0)
                .build();
        WeatherDto risky = safe.toBuilder()
                .uvIndex(9)
                .pollenRiskLevel(3)
                .weatherWarningTitle("폭염경보")
                .weatherWarningDetails("폭염경보 : 서울")
                .build();

        assertThat(risky.getOutingScore()).isLessThan(safe.getOutingScore());
        assertThat(risky.getUvDisplay()).contains("매우 높음");
        assertThat(risky.getPollenDisplay()).contains("잡초류", "매우 높음");
        assertThat(risky.getSafetySummary()).contains("폭염경보", "자외선", "꽃가루");
        assertThat(risky.getSmartAlertSummary()).contains("폭염경보", "자외선", "꽃가루");
    }
}

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
    void materialAdvicePrioritizesColdTemperatureOverHighHumidity() {
        WeatherDto coldHumid = WeatherDto.builder()
                .tmp("1")
                .pop("20")
                .pty("0")
                .sky("3")
                .reh("80")
                .build();
        WeatherDto mildHumid = WeatherDto.builder()
                .tmp("20")
                .pop("0")
                .pty("0")
                .sky("1")
                .reh("80")
                .build();
        WeatherDto hotHumid = WeatherDto.builder()
                .tmp("28")
                .pop("0")
                .pty("0")
                .sky("1")
                .reh("80")
                .build();

        assertThat(coldHumid.getMaterialAdvice()).contains("울", "플리스").doesNotContain("린넨");
        assertThat(mildHumid.getMaterialAdvice()).doesNotContain("린넨");
        assertThat(hotHumid.getMaterialAdvice()).contains("린넨");
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
                .pty("0")
                .sky("1")
                .reh("50")
                .wsd("2.0")
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

    @Test
    void outdoorProfileWeightsWeatherRisksMoreThanDailyProfile() {
        WeatherDto daily = WeatherDto.builder()
                .tmp("29")
                .pop("70")
                .pty("0")
                .sky("1")
                .reh("50")
                .wsd("8.2")
                .uvIndex(9)
                .activityType(ActivityType.DAILY)
                .build();
        WeatherDto outdoor = daily.toBuilder()
                .activityType(ActivityType.OUTDOOR)
                .build();

        assertThat(outdoor.getOutingScore()).isLessThan(daily.getOutingScore());
        assertThat(outdoor.getPersonalizationSummary()).contains("산책·야외 활동");
        assertThat(outdoor.getRiskFactors())
                .isSortedAccordingTo((left, right) -> Integer.compare(right.scoreImpact(), left.scoreImpact()));
    }

    @Test
    void temperatureSensitivityChangesPerceivedRiskAndExplainsEvidence() {
        WeatherDto normal = WeatherDto.builder()
                .tmp("7")
                .pop("10")
                .pty("0")
                .sky("1")
                .reh("50")
                .wsd("2.0")
                .temperatureSensitivity(TemperatureSensitivity.NONE)
                .build();
        WeatherDto coldSensitive = normal.toBuilder()
                .temperatureSensitivity(TemperatureSensitivity.COLD)
                .build();

        assertThat(normal.getRiskFactors()).isEmpty();
        assertThat(coldSensitive.getOutingScore()).isLessThan(normal.getOutingScore());
        assertThat(coldSensitive.getPersonalizedFeelsLikeTemperature()).isEqualTo(4);
        assertThat(coldSensitive.getRiskFactors().get(0).evidence()).contains("기온 7°C", "개인 체감 4°C");
    }

    @Test
    void decisionExplanationNamesPrimaryEvidenceAndAction() {
        WeatherDto risky = WeatherDto.builder()
                .tmp("23")
                .pty("1")
                .pop("80")
                .sky("4")
                .reh("50")
                .wsd("2.0")
                .pm10Grade("3")
                .pm25Grade("2")
                .build();

        assertThat(risky.getRiskFactors()).allSatisfy(factor -> {
            assertThat(factor.code()).isNotBlank();
            assertThat(factor.evidence()).isNotBlank();
            assertThat(factor.action()).isNotBlank();
            assertThat(factor.scoreImpact()).isPositive();
        });
        assertThat(risky.getDecisionExplanation()).contains("가장 큰 변수");
    }

    @Test
    void actionFirstDecisionAndDataQualityStayConsistentForWebAndMail() {
        WeatherDto warning = WeatherDto.builder()
                .tmp("31")
                .pty("1")
                .pop("80")
                .weatherWarningTitle("폭염경보")
                .sourceFieldCount(6)
                .build();

        assertThat(warning.getDecisionHeadline()).isEqualTo("오늘은 안전을 먼저 챙겨요");
        assertThat(warning.getDecisionDetail()).contains("폭염경보");
        assertThat(warning.getDataQualityLabel()).isEqualTo("원천자료 확인");
    }

    @Test
    void fallbackDataIsClearlyMarkedWithoutClaimingForecastConfidence() {
        WeatherDto fallback = WeatherDto.builder()
                .fallbackData(true)
                .sourceFieldCount(6)
                .build();

        assertThat(fallback.getDataQualityLabel()).isEqualTo("마지막 정상자료");
    }

    @Test
    void missingForecastDoesNotInventMorningTemperatureOrFavorableAdvice() {
        WeatherDto missing = WeatherDto.builder().build();

        assertThat(missing.getForecastLabel()).isEqualTo("예보");
        assertThat(missing.getTmp()).isEqualTo("-");
        assertThat(missing.getPersonalizedFeelsLikeTemperature()).isNull();
        assertThat(missing.getSkyDescription()).isEqualTo("자료 없음");
        assertThat(missing.getWeatherMood()).isEqualTo("자료 없음");
        assertThat(missing.getOutingScore()).isEqualTo(-1);
        assertThat(missing.getOutingScoreLabel()).isEqualTo("자료 부족");
        assertThat(missing.getDecisionHeadline()).contains("확인");
        assertThat(missing.getUmbrellaAdvice()).contains("자료가 부족");
        assertThat(missing.getClothingAdvice()).contains("기온 자료가 없어");
        assertThat(missing.getOutdoorAdvice()).contains("일부 예보가 없어");
        assertThat(missing.getWeatherConditionLine()).contains("강수확률 자료 없음", "습도 자료 없음");
    }

    @Test
    void missingTimeKeepsTheSuppliedPeriodInsteadOfPretendingItIsMorning() {
        WeatherDto evening = WeatherDto.builder().date("20261023").periodLabel("저녁").build();

        assertThat(evening.getForecastLabel()).isEqualTo("10월 23일 저녁 예보");
        assertThat(evening.toBuilder().periodLabel(null).build().getForecastLabel())
                .isEqualTo("10월 23일 예보");
    }

    @Test
    void malformedMeasurementsAreNotShownAsNumbersOrUsedForTemperatureRisk() {
        WeatherDto malformed = WeatherDto.builder()
                .tmp("-999").pop("-1").reh("101").wsd("NaN").build();

        assertThat(malformed.getTmp()).isEqualTo("-");
        assertThat(malformed.getPop()).isEqualTo("-");
        assertThat(malformed.getReh()).isEqualTo("-");
        assertThat(malformed.getWsd()).isEqualTo("-");
        assertThat(malformed.getPersonalizedFeelsLikeTemperature()).isNull();
        assertThat(malformed.isTemperatureRisk()).isFalse();
        assertThat(malformed.getRiskFactors()).noneMatch(factor -> "TEMPERATURE".equals(factor.code()));
        assertThat(malformed.getOutingScore()).isEqualTo(-1);
    }

    @Test
    void missingProbabilityNeverSuppressesAKnownRainWarning() {
        WeatherDto rainy = WeatherDto.builder().pty("1").build();

        assertThat(rainy.getUmbrellaAdvice()).isEqualTo("챙기는 것을 추천해요");
        assertThat(rainy.getDecisionHeadline()).contains("우산");
        assertThat(rainy.getSummaryMessage()).contains("비 소식");
        assertThat(rainy.getOutingScoreLabel()).isEqualTo("자료 부족");
    }

    @Test
    void partialAirQualityKeepsKnownBadReadingsWithoutClaimingMissingReadingsAreGood() {
        var partialBad = WeatherDto.builder().pm10Value("90").pm10Grade("3").build();
        assertThat(partialBad.getAirQualitySummary()).contains("미세먼지 나쁨", "초미세먼지 정보 없음");
        assertThat(partialBad.getAirQualityAdvice()).contains("좋지 않아요");
        assertThat(partialBad.getMaskAdvice()).contains("마스크");
        var partialGood = partialBad.toBuilder().pm10Value("10").pm10Grade("1").build();
        assertThat(partialGood.getAirQualityAdvice()).contains("일부 없을");
        assertThat(partialGood.getMaskAdvice()).contains("재확인");
        assertThat(partialGood.toBuilder().pm25Grade("-").build().getAirQualityAdvice()).doesNotContain("좋은 편");
    }
}

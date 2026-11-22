package com.example.WebSideProject.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void missingPeriodsCannotBecomeZeroDegreesOrAnOutdoorRecommendation() {
        WeatherDto missing = WeatherDto.builder().build();
        WeatherPlannerDto planner = WeatherPlannerDto.from("서울", List.of(daily(missing, missing, missing)));

        WeatherPlannerDto.DayPlan plan = planner.days().get(0);
        assertThat(plan.averageScore()).isEqualTo(-1);
        assertThat(plan.scoreLabel()).isEqualTo("자료 없음");
        assertThat(plan.riskLevel()).isEqualTo("UNKNOWN");
        assertThat(plan.bestPeriod()).isEqualTo("UNAVAILABLE");
        assertThat(plan.bestPeriodScore()).isEqualTo(-1);
        assertThat(plan.temperatureRange()).isEqualTo("기온 자료 없음");
        assertThat(plan.keyAction()).contains("자료가 부족").doesNotContain("보온", "야외 일정을");
        assertThat(planner.recommendedDayLabel()).isEqualTo("자료 부족");
        assertThat(planner.recommendedPeriodLabel()).isEqualTo("자료 부족");
        assertThat(planner.headline()).contains("자료가 부족").doesNotContain("가장 움직이기");
        assertThat(planner.packingChecklist()).containsExactly("외출 전 최신 예보 확인");
    }

    @Test
    void partialPeriodsUseOnlyAvailableScoresAndNumericTemperatures() {
        WeatherDto invalid = WeatherDto.builder().tmp("not-a-number").build();
        WeatherDto available = weather("18");
        WeatherDto missing = WeatherDto.builder().tmp("-").build();

        WeatherPlannerDto planner = WeatherPlannerDto.from("서울", List.of(daily(invalid, available, missing)));

        WeatherPlannerDto.DayPlan plan = planner.days().get(0);
        assertThat(plan.averageScore()).isEqualTo(available.getOutingScore());
        assertThat(plan.bestPeriod()).isEqualTo("AFTERNOON");
        assertThat(plan.bestPeriodScore()).isEqualTo(available.getOutingScore());
        assertThat(plan.temperatureRange()).isEqualTo("18°C ~ 18°C");
        assertThat(plan.keyAction()).doesNotContain("보온");
        assertThat(planner.headline()).contains("확인된 예보").doesNotContain("3일 중 가장");
    }

    @Test
    void actualZeroAndNegativeTemperaturesRemainMeasurements() {
        WeatherDto zero = weather("0");
        WeatherDto negative = weather("-1");
        WeatherDto missing = WeatherDto.builder().tmp("-").build();

        WeatherPlannerDto.DayPlan plan = WeatherPlannerDto.from("서울", List.of(daily(zero, negative, missing)))
                .days().get(0);

        assertThat(plan.temperatureRange()).isEqualTo("-1°C ~ 0°C");
        assertThat(plan.averageScore()).isEqualTo(Math.round((zero.getOutingScore() + negative.getOutingScore()) / 2.0f));
        assertThat(plan.riskLevel()).isNotEqualTo("UNKNOWN");
        assertThat(plan.keyAction()).contains("보온");
    }

    @Test
    void aRealZeroScoreIsValidAndDoesNotBecomeTheMissingSentinel() {
        WeatherDto zeroScore = mock(WeatherDto.class);
        when(zeroScore.getOutingScore()).thenReturn(0);
        when(zeroScore.hasCoreWeatherData()).thenReturn(true);
        when(zeroScore.getTmp()).thenReturn("0");
        WeatherDto missing = WeatherDto.builder().build();

        WeatherPlannerDto.DayPlan plan = WeatherPlannerDto.from("서울", List.of(daily(zeroScore, missing, missing)))
                .days().get(0);

        assertThat(plan.averageScore()).isZero();
        assertThat(plan.bestPeriodScore()).isZero();
        assertThat(plan.bestPeriod()).isEqualTo("MORNING");
        assertThat(plan.riskLevel()).isEqualTo("HIGH");
    }

    @Test
    void nonFiniteAndFractionalValuesAreMissingUnderTheIntegerForecastContract() {
        WeatherDto notANumber = WeatherDto.builder().tmp("NaN").build();
        WeatherDto infinite = WeatherDto.builder().tmp("Infinity").build();
        WeatherDto fractional = WeatherDto.builder().tmp("2.5").build();

        WeatherPlannerDto.DayPlan missingPlan = WeatherPlannerDto.from("서울", List.of(daily(notANumber, infinite, fractional)))
                .days().get(0);

        assertThat(missingPlan.temperatureRange()).isEqualTo("기온 자료 없음");
        assertThat(missingPlan.averageScore()).isEqualTo(-1);

        WeatherDto numeric = WeatherDto.builder().tmp("2").build();
        WeatherPlannerDto.DayPlan partialPlan = WeatherPlannerDto.from("서울", List.of(daily(notANumber, numeric, fractional)))
                .days().get(0);
        assertThat(partialPlan.temperatureRange()).isEqualTo("2°C ~ 2°C");
    }

    @Test
    void completePeriodsPreserveTheExistingComparisonContract() {
        WeatherDto morning = weather("18");
        WeatherDto afternoon = weather("23");
        WeatherDto evening = weather("20");
        DailyWeatherDto daily = daily(morning, afternoon, evening);

        WeatherPlannerDto planner = WeatherPlannerDto.from("서울", List.of(daily));
        WeatherPlannerDto.DayPlan plan = planner.days().get(0);

        assertThat(plan.averageScore()).isEqualTo(Math.round(
                (morning.getOutingScore() + afternoon.getOutingScore() + evening.getOutingScore()) / 3.0f));
        assertThat(plan.bestPeriod()).isEqualTo(daily.bestPeriod());
        assertThat(plan.bestPeriodScore()).isEqualTo(daily.bestOutingScore());
        assertThat(plan.temperatureRange()).isEqualTo("18°C ~ 23°C");
        assertThat(planner.headline()).isEqualTo("내일 " + daily.bestPeriodLabel() + "이 3일 중 가장 움직이기 좋아요.");
    }

    private DailyWeatherDto daily(WeatherDto morning, WeatherDto afternoon, WeatherDto evening) {
        return DailyWeatherDto.fromAtHour(morning, afternoon, evening, 0, "내일", "20261024");
    }

    private DailyWeatherDto daily(String label, String date, WeatherDto morning, WeatherDto afternoon, WeatherDto evening) {
        return DailyWeatherDto.fromAtHour(morning, afternoon, evening, 0, label, date);
    }

    private WeatherDto weather(String temperature) {
        return WeatherDto.builder().tmp(temperature).pop("0").pty("0").sky("1").reh("50").wsd("0").build();
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

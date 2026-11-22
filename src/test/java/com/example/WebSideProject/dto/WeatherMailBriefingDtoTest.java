package com.example.WebSideProject.dto;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherMailBriefingDtoTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-23T09:35:00Z"), ZoneOffset.UTC);

    @Test
    void usesKoreanTimeAndShowsOnlyUpcomingTimesInOrderLimitedToSix() {
        var weather = WeatherDto.builder().date("20261023").time("2100").tmp("15").build();
        List<HourlyWeatherDto.Hour> hours = IntStream.rangeClosed(0, 23)
                .mapToObj(hour -> hour(String.format("%02d:00", 23 - hour))).toList();
        Clock afternoon = Clock.fixed(Instant.parse("2026-10-23T06:35:00Z"), ZoneOffset.UTC);
        var briefing = WeatherMailBriefingDto.from(weather, hourly("2026-10-23", hours), afternoon);
        assertThat(briefing.hours()).extracting(WeatherMailBriefingDto.Hour::time)
                .containsExactly("16:00", "17:00", "18:00", "19:00", "20:00", "21:00");
        assertThat(briefing.forecastTimeLabel()).isEqualTo("21:00 예보");
    }

    @Test
    void excludesTheElapsedHourButKeepsAnExactlyMatchingForecastMinute() {
        WeatherDto weather = WeatherDto.builder().date("20261023").build();
        var hourly = hourly("2026-10-23", List.of(hour("18:00"), hour("18:45"), hour("19:00")));
        Clock at1845 = Clock.fixed(Instant.parse("2026-10-23T09:45:35Z"), ZoneOffset.UTC);
        var briefing = WeatherMailBriefingDto.from(weather, hourly, at1845);
        assertThat(briefing.hours()).extracting(WeatherMailBriefingDto.Hour::time).containsExactly("18:45", "19:00");
    }

    @Test
    void comparesFullKoreanDateAtMidnightInsteadOfOnlyTheClockHour() {
        Clock afterMidnight = Clock.fixed(Instant.parse("2026-10-23T15:05:00Z"), ZoneOffset.UTC);
        WeatherDto yesterday = WeatherDto.builder().date("20261023").build();
        var briefing = WeatherMailBriefingDto.from(yesterday, hourly("2026-10-23", List.of(hour("23:00"))), afterMidnight);
        assertThat(briefing.hours()).isEmpty();
        assertThat(briefing.hourlyNotice()).contains("지난 날짜");
        assertThat(WeatherMailBriefingDto.hourlyDayOffset(yesterday, afterMidnight)).isEqualTo(-1);
        assertThat(WeatherMailBriefingDto.hourlyDayOffset(WeatherDto.builder().date("20261024").build(), afterMidnight)).isZero();
    }

    @Test
    void neverCombinesWeatherForOneDateWithAnotherDatesHourlyMeasurements() {
        WeatherDto tomorrow = WeatherDto.builder().date("20261024").build();
        var briefing = WeatherMailBriefingDto.from(tomorrow, hourly("2026-10-23", List.of(hour("22:00"))), CLOCK);
        assertThat(briefing.hours()).isEmpty();
        assertThat(WeatherMailBriefingDto.hourlyDayOffset(tomorrow, CLOCK)).isEqualTo(1);
    }

    @Test
    void keepsMissingMeasurementsMissingRatherThanTurningThemIntoGoodWeather() {
        WeatherDto missing = WeatherDto.builder().date("20261023").build();
        var source = new HourlyWeatherDto.Hour("19:00", null, null, "자료 없음", null, null, "자료 없음");
        var briefing = WeatherMailBriefingDto.from(missing, hourly("2026-10-23", List.of(source)), CLOCK);
        assertThat(briefing.temperature()).isEqualTo("자료 없음");
        assertThat(briefing.precipitationSummary()).isEqualTo("자료 없음");
        assertThat(briefing.condition()).isEqualTo("자료 없음");
        assertThat(briefing.hours().get(0).temperature()).isEqualTo("자료 없음");
        assertThat(briefing.hours().get(0).precipitation()).isEqualTo("자료 없음");
        assertThat(briefing.plainText("강남역", "https://weather.coders.kr", "https://weather.coders.kr/stop"))
                .doesNotContain("20°C", "0%", "맑음", "외출 100", "내일은");
    }

    @Test
    void rainHeadlineAndProbabilityOnlyUseRelevantUpcomingHours() {
        WeatherDto weather = WeatherDto.builder().date("20261023").pty("0").pop("10").tmp("15").build();
        var oldRain = new HourlyWeatherDto.Hour("09:00", 12, 100, "비", 90, 2.4, "흐림");
        var futureRain = new HourlyWeatherDto.Hour("20:00", 14, 70, "비", 90, 2.4, "흐림");
        var briefing = WeatherMailBriefingDto.from(weather, hourly("2026-10-23", List.of(oldRain, futureRain)), CLOCK);
        assertThat(briefing.headline()).startsWith("20시");
        assertThat(briefing.precipitationSummary()).contains("70%").doesNotContain("100%");
    }

    @Test
    void lateDeliveryDoesNotTurnAPastPeriodsRainIntoUpcomingUmbrellaAdvice() {
        Clock night = Clock.fixed(Instant.parse("2026-10-23T13:35:00Z"), ZoneOffset.UTC);
        WeatherDto morningRain = WeatherDto.builder().date("20261023").time("0900")
                .pty("1").sky("4").pop("90").tmp("12").build();
        var briefing = WeatherMailBriefingDto.from(morningRain,
                hourly("2026-10-23", List.of(hour("23:00"))), night);
        assertThat(briefing.headline()).contains("15°C").doesNotContain("우산", "비나 눈");
        assertThat(briefing.precipitationSummary()).contains("10%").doesNotContain("90%");
        assertThat(briefing.forecastTimeLabel()).isEqualTo("09:00 예보");

        var withoutHourly = WeatherMailBriefingDto.from(morningRain, null, night);
        assertThat(withoutHourly.headline()).contains("지난 시각").doesNotContain("우산");
        assertThat(withoutHourly.precipitationSummary()).isEqualTo("90% · 선택 시각 기준");
    }

    @Test
    void upcomingSelectedForecastKeepsItsExactRainTimeAndTomorrowLabels() {
        WeatherDto tomorrow = WeatherDto.builder().date("20261024").time("0900")
                .pty("1").sky("4").pop("90").tmp("12").build();
        var briefing = WeatherMailBriefingDto.from(tomorrow, null, CLOCK);
        assertThat(briefing.editionLabel()).isEqualTo("내일의 날씨 편지");
        assertThat(briefing.summaryLabel()).isEqualTo("내일의 핵심");
        assertThat(briefing.headline()).isEqualTo("9시 예보에는 비나 눈 소식이 있어요.");
        assertThat(briefing.plainText("강남역", "https://weather.coders.kr", "https://weather.coders.kr/stop"))
                .contains("내일의 핵심").doesNotContain("오늘의 핵심");

        var warning = WeatherMailBriefingDto.from(tomorrow.toBuilder().weatherWarningTitle("호우주의보").build(), null, CLOCK);
        assertThat(warning.headline()).isEqualTo("기상특보를 먼저 확인해 주세요.");
    }

    @Test
    void showsPartialAirQualityAndConvertsSourceTimesToKoreanTime() {
        WeatherDto weather = WeatherDto.builder().date("20261023").pm10Grade("3")
                .forecastIssuedAt("2026-10-23T02:00:00Z").dataFetchedAt("2026-10-23T18:00:00+09:00").build();
        var briefing = WeatherMailBriefingDto.from(weather, null, CLOCK);
        assertThat(briefing.airQualitySummary()).isEqualTo("미세 나쁨 · 초미세 자료 없음");
        assertThat(briefing.issuedAtLabel()).isEqualTo("10월 23일 11:00 KST");
        assertThat(briefing.fetchedAtLabel()).isEqualTo("10월 23일 18:00 KST");
    }

    @Test
    void omitsMalformedHourlyTimesAndRejectsUnsupportedRequestDates() {
        WeatherDto weather = WeatherDto.builder().date("20261023").build();
        var briefing = WeatherMailBriefingDto.from(weather,
                hourly("2026-10-23", List.of(hour("not-a-time"), hour("24:99"), hour("19:00"))), CLOCK);
        assertThat(briefing.hours()).extracting(WeatherMailBriefingDto.Hour::time).containsExactly("19:00");
        assertThat(WeatherMailBriefingDto.hourlyDayOffset(WeatherDto.builder().date("invalid").build(), CLOCK)).isEqualTo(-1);
        assertThat(WeatherMailBriefingDto.hourlyDayOffset(WeatherDto.builder().date("20261026").build(), CLOCK)).isEqualTo(-1);
    }

    private HourlyWeatherDto.Hour hour(String time) { return new HourlyWeatherDto.Hour(time, 15, 10, "없음", 60, 2.4, "구름 많음"); }
    private HourlyWeatherDto hourly(String date, List<HourlyWeatherDto.Hour> hours) {
        return new HourlyWeatherDto("강남역", date, "", "", 12, 21, 10, hours, null);
    }
}

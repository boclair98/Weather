package com.example.WebSideProject.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RouteBriefingDtoTest {

    @Test
    void buildsDoorToDoorChecklistFromDestinationWeather() {
        LocationDto.Response origin = LocationDto.Response.builder()
                .locationName("강남역")
                .regionName("서울 강남구")
                .build();
        LocationDto.Response destination = LocationDto.Response.builder()
                .locationName("서울시청")
                .regionName("서울 중구")
                .build();
        WeatherDto originWeather = WeatherDto.builder().tmp("28").pop("10").build();
        WeatherDto destinationWeather = WeatherDto.builder()
                .tmp("23")
                .pop("80")
                .uvIndex(9)
                .pollenType("잡초류")
                .pollenRiskLevel(2)
                .build();

        RouteBriefingDto briefing = RouteBriefingDto.from(
                origin, destination, 12400, 4380, originWeather, destinationWeather);

        assertThat(briefing.distanceLabel()).isEqualTo("12.4km");
        assertThat(briefing.durationLabel()).isEqualTo("1시간 13분");
        assertThat(briefing.checklist()).anyMatch(item -> item.contains("우산"));
        assertThat(briefing.checklist()).anyMatch(item -> item.contains("자외선"));
        assertThat(briefing.checklist()).anyMatch(item -> item.contains("기온 차"));
    }

    @Test
    void missingTemperaturesDoNotCreateATwentyDegreeDefaultOrFavorableAssurances() {
        RouteBriefingDto partial = briefing(weather(null), weather("30"));
        RouteBriefingDto missing = briefing(weather(null), weather(null));

        assertThat(partial.checklist()).anyMatch(item -> item.contains("기온 자료가 없어"));
        assertThat(partial.checklist()).noneMatch(item -> item.contains("기온 차가"));
        assertThat(partial.headline()).contains("자료가 부족");
        assertThat(missing.checklist()).noneMatch(item -> item.contains("변수가 적") || item.contains("변수는 적"));
        assertThat(missing.headline()).doesNotContain("무난");
    }

    @Test
    void zeroDegreesIsARealMeasurementWhenComparingTheTwoLocations() {
        RouteBriefingDto briefing = briefing(weather("0"), weather("5"));

        assertThat(briefing.originWeather().getTmp()).isEqualTo("0");
        assertThat(briefing.checklist()).anyMatch(item -> item.contains("기온 차가 5°C"));
        assertThat(briefing.checklist()).noneMatch(item -> item.contains("기온 자료가 없어"));
    }

    @Test
    void missingAndUnsupportedPrecipitationTypesRemainUnknownRatherThanDry() {
        RouteBriefingDto briefing = briefing(
                weather("20").toBuilder().pty(null).build(),
                weather("20").toBuilder().pty("9").build());

        assertThat(briefing.checklist()).anyMatch(item -> item.contains("강수 자료가 부족"));
        assertThat(briefing.checklist()).noneMatch(item -> item.contains("변수가 적") || item.contains("변수는 적"));
        assertThat(briefing.headline()).contains("자료가 부족").doesNotContain("무난");
    }

    private RouteBriefingDto briefing(WeatherDto origin, WeatherDto destination) {
        return RouteBriefingDto.from(
                LocationDto.Response.builder().locationName("출발지").build(),
                LocationDto.Response.builder().locationName("목적지").build(),
                1000, 600, origin, destination);
    }

    private WeatherDto weather(String temperature) {
        return WeatherDto.builder().tmp(temperature).pop("0").pty("0")
                .sky("1").reh("50").wsd("2.0").build();
    }
}

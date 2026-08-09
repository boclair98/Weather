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
}

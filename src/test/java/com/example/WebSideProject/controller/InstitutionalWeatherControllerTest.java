package com.example.WebSideProject.controller;

import com.example.WebSideProject.dto.ForecastProvenanceDto;
import com.example.WebSideProject.dto.WeatherPlannerDto;
import com.example.WebSideProject.service.WeatherPlannerService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InstitutionalWeatherControllerTest {

    @Test
    void returnsVersionedTraceableDataQualityContract() {
        WeatherPlannerService service = mock(WeatherPlannerService.class);
        ForecastProvenanceDto provenance = new ForecastProvenanceDto(
                "기상청 단기예보", "https://www.weather.go.kr", "2026-08-10T02:00+09:00",
                "2026-08-09T17:00:00Z", 10, "FRESH", "VERIFIED", 100, false, "notice"
        );
        WeatherPlannerDto planner = new WeatherPlannerDto(
                "서울시청", "오늘", "아침", "추천", List.of(), List.of(), provenance
        );
        when(service.getPlanner(anyInt(), anyInt(), any(), any(), any(), any(), any()))
                .thenReturn(planner);
        InstitutionalWeatherController controller = new InstitutionalWeatherController(service);

        ResponseEntity<?> response = controller.getBriefing(
                60, 127, "서울시청", null, null, null, null
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst("X-Schema-Version"))
                .isEqualTo("weather-briefing/1.0");
        assertThat(response.getHeaders().getFirst("X-Data-Source")).isEqualTo("KMA_VILAGE_FORECAST");
        assertThat(response.getHeaders().getFirst("X-Data-Quality")).isEqualTo("VERIFIED");
        assertThat(response.getBody()).isNotNull();
    }
}

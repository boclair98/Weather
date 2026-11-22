package com.example.WebSideProject.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeatherSafetyServiceTest {

    private final WeatherSafetyService service = new WeatherSafetyService(new RestTemplate());

    @Test
    void parsesMaximumUvForRequestedDate() {
        String response = """
                {"response":{"body":{"items":{"item":[{
                  "date":"2026080915","h0":"4","h3":"1","h6":"0",
                  "h9":"0","h12":"0","h15":"1","h18":"7","h21":"8","h24":"5"
                }]}}}}
                """;

        assertThat(service.parseUvIndex(response, LocalDate.of(2026, 8, 10))).isEqualTo(8);
    }

    @Test
    void parsesPollenAndRegionalWarning() {
        String pollen = """
                {"response":{"body":{"items":{"item":[{
                  "today":"0","tomorrow":"2","dayaftertomorrow":"3"
                }]}}}}
                """;
        String warning = """
                {"response":{"body":{"items":{"item":[{
                  "t6":"o 폭염경보 : 경기도 일부, 서울\\r\\no 강풍주의보 : 제주도"
                }]}}}}
                """;

        assertThat(service.parsePollen(pollen, 1, "잡초류").level()).isEqualTo(2);
        assertThat(service.parseWarning(warning, "서울").title()).isEqualTo("폭염경보");
        assertThat(service.parseWarning(warning, "부산").title()).isEqualTo("-");
    }

    @Test
    void usesKoreanDateHourAndPollenSeasonAtTheUtcMonthBoundary() {
        RestTemplate rest = mock(RestTemplate.class);
        when(rest.getForObject(any(URI.class), eq(String.class))).thenReturn("""
                {"response":{"body":{"items":{"item":[{
                  "date":"2026030100","h0":"2","today":"1","t6":"o 강풍주의보 : 서울"
                }]}}}}
                """);
        WeatherSafetyService safety = new WeatherSafetyService(rest);
        ReflectionTestUtils.setField(safety, "clock",
                Clock.fixed(Instant.parse("2026-02-28T15:05:00Z"), ZoneOffset.UTC));
        ReflectionTestUtils.setField(safety, "uvApiKey", "test-key");
        ReflectionTestUtils.setField(safety, "pollenApiKey", "test-key");
        ReflectionTestUtils.setField(safety, "warningApiKey", "test-key");
        ReflectionTestUtils.setField(safety, "uvBaseUrl", "https://weather.example.test/uv");
        ReflectionTestUtils.setField(safety, "pollenBaseUrl", "https://weather.example.test/pollen");
        ReflectionTestUtils.setField(safety, "warningBaseUrl", "https://weather.example.test/warning");

        safety.getSafetyInsight("서울", 0);

        ArgumentCaptor<URI> requests = ArgumentCaptor.forClass(URI.class);
        verify(rest, times(3)).getForObject(requests.capture(), eq(String.class));
        assertThat(requests.getAllValues().get(0).getQuery()).contains("time=2026030100");
        assertThat(requests.getAllValues().get(1).getPath()).endsWith("/getOakPollenRiskIdxV3");
        assertThat(requests.getAllValues().get(1).getQuery()).contains("time=2026030100");
        assertThat(requests.getAllValues().get(2).getQuery())
                .contains("fromTmFc=20260301", "toTmFc=20260301");
    }
}

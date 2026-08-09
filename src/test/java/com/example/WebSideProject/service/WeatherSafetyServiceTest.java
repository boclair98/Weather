package com.example.WebSideProject.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

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
}

package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.WeatherDecisionDto;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class WeatherDecisionServiceTest {

    @Test
    void recommendsDryerDepartureAndExplainsTheImprovement() {
        WeatherService service = service(forecastResponse());

        WeatherDecisionDto decision = service.getDecisionWindow(
                60, 127, "을지로3가", LocalDate.now().toString(), "18:00", 60, 60
        );

        assertThat(decision.recommendedStart()).isEqualTo("19:00");
        assertThat(decision.shiftMinutes()).isEqualTo(60);
        assertThat(decision.improvementPoints()).isGreaterThan(40);
        assertThat(decision.rationale()).contains("강수확률 90% → 10%", "비·눈 시간대 회피");
        assertThat(decision.candidates()).hasSize(3);
        assertThat(decision.candidates()).filteredOn(WeatherDecisionDto.WindowCandidate::recommended)
                .singleElement().extracting(WeatherDecisionDto.WindowCandidate::start)
                .isEqualTo("19:00");
        assertThat(decision.provenance().qualityStatus()).isEqualTo("VERIFIED");
    }

    @Test
    void rejectsUnsupportedScheduleRange() {
        WeatherService service = service(forecastResponse());

        assertThatThrownBy(() -> service.getDecisionWindow(
                60, 127, "을지로3가", LocalDate.now().toString(), "18:00", 240, 60
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("180분");
    }

    private WeatherService service(String response) {
        WeatherService service = new WeatherService(
                new StubRestTemplate(response),
                mock(WeatherSafetyService.class),
                new ExternalApiGuard(
                        new SimpleMeterRegistry(), Runnable::run, java.time.Duration.ofSeconds(1)
                )
        );
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://weather.example.test");
        return service;
    }

    private String forecastResponse() {
        JSONArray items = new JSONArray();
        String date = LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        addHour(items, date, "1700", "20", "20", "0", "7.5");
        addHour(items, date, "1800", "19", "90", "1", "3.0");
        addHour(items, date, "1900", "19", "10", "0", "2.0");
        return new JSONObject()
                .put("response", new JSONObject()
                        .put("header", new JSONObject().put("resultCode", "00"))
                        .put("body", new JSONObject()
                                .put("items", new JSONObject().put("item", items))))
                .toString();
    }

    private void addHour(
            JSONArray items,
            String date,
            String time,
            String temperature,
            String pop,
            String pty,
            String wind
    ) {
        for (List<String> field : List.of(
                List.of("TMP", temperature), List.of("POP", pop), List.of("PTY", pty),
                List.of("REH", "60"), List.of("WSD", wind), List.of("SKY", "3")
        )) {
            items.put(new JSONObject()
                    .put("fcstDate", date)
                    .put("fcstTime", time)
                    .put("category", field.get(0))
                    .put("fcstValue", field.get(1)));
        }
    }

    private static final class StubRestTemplate extends RestTemplate {
        private final String response;

        private StubRestTemplate(String response) {
            this.response = response;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getForObject(URI url, Class<T> responseType) {
            return (T) response;
        }
    }
}

package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.CurrentWeatherDto;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class CurrentWeatherServiceTest {

    @Test
    void parsesKmaUltraShortObservation() {
        WeatherService service = service(observationResponse());

        CurrentWeatherDto current = service.parseCurrentWeather(observationResponse(), "강남역");

        assertThat(current.locationName()).isEqualTo("강남역");
        assertThat(current.temperature()).isEqualTo(27.4);
        assertThat(current.humidity()).isEqualTo(78);
        assertThat(current.windSpeed()).isEqualTo(3.2);
        assertThat(current.precipitationType()).isEqualTo("비");
        assertThat(current.oneHourPrecipitation()).isEqualTo("1.5");
        assertThat(current.observedAt()).isEqualTo("2026-08-13T14:00+09:00");
    }

    @Test
    void rejectsObservationWithoutRequiredFields() {
        WeatherService service = service(observationResponse());
        JSONArray items = new JSONArray().put(item("PTY", "0"));
        String incomplete = response(items);

        assertThatThrownBy(() -> service.parseCurrentWeather(incomplete, "서울"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("필수 관측값");
    }

    private WeatherService service(String response) {
        WeatherService service = new WeatherService(
                new StubRestTemplate(response), mock(WeatherSafetyService.class),
                new ExternalApiGuard(new SimpleMeterRegistry(), Runnable::run, Duration.ofSeconds(1))
        );
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://weather.example.test");
        return service;
    }

    private String observationResponse() {
        JSONArray items = new JSONArray();
        for (List<String> field : List.of(
                List.of("T1H", "27.4"), List.of("REH", "78"), List.of("WSD", "3.2"),
                List.of("PTY", "1"), List.of("RN1", "1.5")
        )) {
            items.put(item(field.get(0), field.get(1)));
        }
        return response(items);
    }

    private JSONObject item(String category, String value) {
        return new JSONObject()
                .put("baseDate", "20260813")
                .put("baseTime", "1400")
                .put("category", category)
                .put("obsrValue", value);
    }

    private String response(JSONArray items) {
        return new JSONObject().put("response", new JSONObject()
                .put("header", new JSONObject().put("resultCode", "00"))
                .put("body", new JSONObject().put("items", new JSONObject().put("item", items))))
                .toString();
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

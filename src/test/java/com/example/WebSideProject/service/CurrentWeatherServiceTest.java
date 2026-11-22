package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.CurrentWeatherDto;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
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

    @Test
    void missingPrecipitationIsNotReportedAsZeroOrNoRain() {
        JSONArray items = new JSONArray().put(item("T1H", "20"))
                .put(item("REH", "50")).put(item("WSD", "0"));
        var current = service(response(items)).parseCurrentWeather(response(items), "서울");
        assertThat(current.oneHourPrecipitation()).isEqualTo("-");
        assertThat(current.precipitationType()).isEqualTo("정보 없음");
        assertThat(current.headline()).isEqualTo("강수 관측 정보 없음");
        assertThat(current.advice()).doesNotContain("강수가 확인", "큰 불편 신호는 없");
    }

    @Test
    void malformedPrecipitationKeepsValidTemperatureButDoesNotInventRain() {
        JSONArray items = new JSONArray().put(item("T1H", "0"))
                .put(item("REH", "0")).put(item("WSD", "0"))
                .put(item("RN1", "-999")).put(item("PTY", "9"));
        var current = service(response(items)).parseCurrentWeather(response(items), "서울");
        assertThat(current.temperature()).isZero();
        assertThat(current.oneHourPrecipitation()).isEqualTo("-");
        assertThat(current.precipitationType()).isEqualTo("정보 없음");
    }

    @Test
    void rejectsNonFiniteAndOutOfRangeCoreObservations() {
        for (List<String> invalid : List.of(List.of("NaN", "50", "1"), List.of("Infinity", "50", "1"),
                List.of("20", "101", "1"), List.of("20", "-1", "1"), List.of("20", "50", "-1"),
                List.of("-999", "50", "1"))) {
            JSONArray items = new JSONArray().put(item("T1H", invalid.get(0)))
                    .put(item("REH", invalid.get(1))).put(item("WSD", invalid.get(2)));
            assertThatThrownBy(() -> service(response(items)).parseCurrentWeather(response(items), "서울"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void currentCacheKeyIncludesSelectedLocationForSharedGrid() throws Exception {
        var annotation = WeatherService.class.getMethod("getCurrentWeather", int.class, int.class, String.class)
                .getAnnotation(Cacheable.class);
        var context = new StandardEvaluationContext();
        context.setVariable("nx", 60);
        context.setVariable("ny", 127);
        var expression = new SpelExpressionParser().parseExpression(annotation.key());
        context.setVariable("locationName", "을지로3가역");
        String first = expression.getValue(context, String.class);
        context.setVariable("locationName", "종로3가역");
        assertThat(expression.getValue(context, String.class)).isNotEqualTo(first);
    }

    @Test
    void fallbackSnapshotCannotLeakAnotherLocationNameOnTheSameGrid() {
        RestTemplate client = mock(RestTemplate.class);
        org.mockito.Mockito.when(client.getForObject(org.mockito.ArgumentMatchers.any(URI.class), org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn(observationResponse()).thenThrow(new IllegalStateException("fixture outage"));
        var service = new WeatherService(client, mock(WeatherSafetyService.class),
                new ExternalApiGuard(new SimpleMeterRegistry(), Runnable::run, Duration.ofSeconds(1)));
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://weather.example.test");
        service.getCurrentWeather(60, 127, "을지로3가역");
        assertThatThrownBy(() -> service.getCurrentWeather(60, 127, "종로3가역"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(service.getCurrentWeather(60, 127, "을지로3가역").locationName()).isEqualTo("을지로3가역");
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

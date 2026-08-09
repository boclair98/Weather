package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.DailyWeatherDto;
import com.example.WebSideProject.dto.ForecastProvenanceDto;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class WeatherServicePlannerTest {

    @Test
    void buildsThreeDaysFromOneForecastRequest() {
        CountingRestTemplate restTemplate = new CountingRestTemplate(forecastResponse());
        WeatherService service = new WeatherService(
                restTemplate,
                mock(WeatherSafetyService.class),
                new ExternalApiGuard(
                        new SimpleMeterRegistry(), Runnable::run, java.time.Duration.ofSeconds(1)
                )
        );
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://weather.example.test");

        List<DailyWeatherDto> forecasts = service.getPlannerDailyWeatherList(60, 127, "서울시청");

        assertThat(forecasts).hasSize(3);
        assertThat(forecasts).extracting(DailyWeatherDto::dayLabel)
                .containsExactly("오늘", "내일", "모레");
        assertThat(restTemplate.requestCount()).isEqualTo(1);
        ForecastProvenanceDto provenance = ForecastProvenanceDto.from(forecasts);
        assertThat(provenance.sourceName()).isEqualTo("기상청 단기예보");
        assertThat(provenance.completenessPercent()).isEqualTo(100);
        assertThat(provenance.qualityStatus()).isEqualTo("VERIFIED");
        assertThat(provenance.fallback()).isFalse();
    }

    @Test
    void servesClearlyMarkedLastKnownGoodForecastWhenProviderFails() {
        FailingAfterFirstRestTemplate restTemplate = new FailingAfterFirstRestTemplate(forecastResponse());
        WeatherService service = service(restTemplate);

        service.getPlannerDailyWeatherList(60, 127, "서울시청");
        List<DailyWeatherDto> fallback = service.getPlannerDailyWeatherList(60, 127, "서울시청");

        ForecastProvenanceDto provenance = ForecastProvenanceDto.from(fallback);
        assertThat(provenance.fallback()).isTrue();
        assertThat(provenance.freshness()).isEqualTo("STALE_FALLBACK");
        assertThat(provenance.qualityStatus()).isEqualTo("DEGRADED");
        assertThat(restTemplate.requestCount()).isEqualTo(3);
    }

    @Test
    void reportsServiceUnavailableWhenProviderAndFallbackAreUnavailable() {
        WeatherService service = service(new AlwaysFailingRestTemplate());

        assertThatThrownBy(() -> service.getPlannerDailyWeatherList(60, 127, "서울시청"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("최근 자료도 없습니다");
    }

    @Test
    void reportsPartialQualityFromActuallyMissingSourceFields() {
        WeatherService service = service(new CountingRestTemplate(forecastResponse(false)));

        ForecastProvenanceDto provenance = ForecastProvenanceDto.from(
                service.getPlannerDailyWeatherList(60, 127, "서울시청")
        );

        assertThat(provenance.completenessPercent()).isEqualTo(83);
        assertThat(provenance.qualityStatus()).isEqualTo("PARTIAL");
    }

    private WeatherService service(RestTemplate restTemplate) {
        WeatherService service = new WeatherService(
                restTemplate,
                mock(WeatherSafetyService.class),
                new ExternalApiGuard(
                        new SimpleMeterRegistry(), Runnable::run, java.time.Duration.ofSeconds(1)
                )
        );
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://weather.example.test");
        return service;
    }

    @Test
    void plannerResultAndSourceUseDifferentCacheLocks() throws NoSuchMethodException {
        Cacheable plannerCache = WeatherPlannerService.class
                .getMethod(
                        "getPlanner", int.class, int.class, String.class,
                        com.example.WebSideProject.Enum.AgeGroup.class,
                        com.example.WebSideProject.Enum.GenderType.class,
                        com.example.WebSideProject.Enum.TemperatureSensitivity.class,
                        com.example.WebSideProject.Enum.ActivityType.class
                )
                .getAnnotation(Cacheable.class);
        Cacheable sourceCache = WeatherService.class
                .getMethod("getPlannerDailyWeatherList", int.class, int.class, String.class)
                .getAnnotation(Cacheable.class);

        assertThat(plannerCache.cacheNames()).containsExactly("weather");
        assertThat(sourceCache.cacheNames()).containsExactly("plannerSource");
    }

    private String forecastResponse() {
        return forecastResponse(true);
    }

    private String forecastResponse(boolean includeWind) {
        JSONArray items = new JSONArray();
        for (int offset = 0; offset <= 2; offset++) {
            String date = LocalDate.now().plusDays(offset).format(DateTimeFormatter.BASIC_ISO_DATE);
            for (String time : List.of("0900", "1500", "2100")) {
                items.put(item(date, time, "SKY", "1"));
                items.put(item(date, time, "PTY", "0"));
                items.put(item(date, time, "TMP", String.valueOf(20 + offset)));
                items.put(item(date, time, "POP", "10"));
                items.put(item(date, time, "REH", "55"));
                if (includeWind) items.put(item(date, time, "WSD", "2.1"));
            }
        }
        return new JSONObject()
                .put("response", new JSONObject()
                        .put("header", new JSONObject()
                                .put("resultCode", "00")
                                .put("resultMsg", "NORMAL_SERVICE"))
                        .put("body", new JSONObject()
                                .put("items", new JSONObject().put("item", items))))
                .toString();
    }

    private JSONObject item(String date, String time, String category, String value) {
        return new JSONObject()
                .put("fcstDate", date)
                .put("fcstTime", time)
                .put("category", category)
                .put("fcstValue", value);
    }

    private static final class CountingRestTemplate extends RestTemplate {
        private final String response;
        private final AtomicInteger requests = new AtomicInteger();

        private CountingRestTemplate(String response) {
            this.response = response;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getForObject(URI url, Class<T> responseType) {
            requests.incrementAndGet();
            return (T) response;
        }

        private int requestCount() {
            return requests.get();
        }
    }

    private static final class FailingAfterFirstRestTemplate extends RestTemplate {
        private final String response;
        private final AtomicInteger requests = new AtomicInteger();

        private FailingAfterFirstRestTemplate(String response) {
            this.response = response;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getForObject(URI url, Class<T> responseType) {
            if (requests.incrementAndGet() > 1) {
                throw new IllegalStateException("provider unavailable");
            }
            return (T) response;
        }

        private int requestCount() {
            return requests.get();
        }
    }

    private static final class AlwaysFailingRestTemplate extends RestTemplate {
        @Override
        public <T> T getForObject(URI url, Class<T> responseType) {
            throw new IllegalStateException("provider unavailable");
        }
    }
}

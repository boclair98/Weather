package com.example.WebSideProject.service;

import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.dto.HourlyWeatherDto;
import com.example.WebSideProject.dto.SafetyInsightDto;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

class WeatherServiceTimezoneTest {

    @Test
    void usesTheKoreanDateAndPreviousIssueAtMidnightEvenWithAUtcClock() {
        ForecastRestTemplate rest = new ForecastRestTemplate();
        WeatherService service = service(rest, "2026-10-23T15:05:00Z");

        HourlyWeatherDto hourly = service.getHourlyWeather(60, 127, "서울시청", 0);

        assertThat(hourly.forecastDate()).isEqualTo("2026-10-24");
        assertThat(rest.lastRequest.getQuery()).contains("base_date=20261023", "base_time=2300");

        assertThat(service.getWeather(60, 127, WeatherPeriod.MORNING, "서울시청").getDate())
                .isEqualTo("20261024");
        assertThat(rest.lastRequest.getQuery()).contains("base_date=20261023", "base_time=2300");
        assertThat(service.getPlannerDailyWeatherList(60, 127, "서울시청"))
                .extracting(day -> day.forecastDate())
                .containsExactly("20261024", "20261025", "20261026");
    }

    @Test
    void switchesTheDailyIssueAt0210KoreanTimeRatherThan0210Utc() {
        ForecastRestTemplate rest = new ForecastRestTemplate();
        WeatherService service = service(rest, "2026-10-23T17:09:00Z");
        service.getHourlyWeather(60, 127, "서울시청", 0);
        assertThat(rest.lastRequest.getQuery()).contains("base_date=20261023", "base_time=2300");

        ReflectionTestUtils.setField(service, "clock", utcClock("2026-10-23T17:10:00Z"));
        service.getHourlyWeather(60, 127, "서울시청", 0);
        assertThat(rest.lastRequest.getQuery()).contains("base_date=20261024", "base_time=0200");
    }

    @Test
    void validatesDecisionDatesAgainstTheKoreanDayAtUtcDateBoundaries() {
        WeatherService service = service(new ForecastRestTemplate(), "2026-10-23T15:05:00Z");

        assertThatThrownBy(() -> service.getDecisionWindow(
                60, 127, "서울시청", "2026-10-23", "18:00", 60, 60
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("오늘부터 모레");
    }

    @Test
    void actualCacheInterceptorDoesNotReuseRelativeForecastsAcrossKoreanMidnight() {
        ForecastRestTemplate rest = new ForecastRestTemplate();
        WeatherService target = service(rest, "2026-10-24T14:59:00Z");
        CacheInterceptor interceptor = new CacheInterceptor();
        interceptor.setCacheManager(new ConcurrentMapCacheManager("weather", "plannerSource"));
        interceptor.setCacheOperationSources(new AnnotationCacheOperationSource());
        interceptor.afterPropertiesSet();
        interceptor.afterSingletonsInstantiated();
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(interceptor);
        WeatherService cached = (WeatherService) factory.getProxy();

        assertThat(cached.getHourlyWeather(60, 127, "서울시청", 0).forecastDate()).isEqualTo("2026-10-24");
        assertThat(cached.getDailyWeather(60, 127, "서울시청", 0).forecastDate()).isEqualTo("20261024");
        cached.getDailyWeather(60, 127, "서울시청", 1);
        cached.getPlannerDailyWeatherList(60, 127, "서울시청");
        cached.getWeather(60, 127, WeatherPeriod.MORNING, "서울시청");
        int beforeMidnight = rest.requests;
        cached.getHourlyWeather(60, 127, "서울시청", 0);
        assertThat(rest.requests).isEqualTo(beforeMidnight);

        ReflectionTestUtils.setField(target, "clock", utcClock("2026-10-24T15:01:00Z"));
        assertThat(cached.getHourlyWeather(60, 127, "서울시청", 0).forecastDate()).isEqualTo("2026-10-25");
        var today = cached.getDailyWeather(60, 127, "서울시청", 0);
        assertThat(today.forecastDate()).isEqualTo("20261025");
        assertThat(today.dayLabel()).isEqualTo("오늘");
        assertThat(cached.getPlannerDailyWeatherList(60, 127, "서울시청").get(0).forecastDate()).isEqualTo("20261025");
        assertThat(cached.getWeather(60, 127, WeatherPeriod.MORNING, "서울시청").getDate()).isEqualTo("20261025");
        assertThat(rest.requests).isEqualTo(beforeMidnight + 4);
    }

    @Test
    void sameDateFallbackIsPreservedButPreviousDateOnlySnapshotIsRejectedAfterMidnight() {
        ForecastRestTemplate rest = new ForecastRestTemplate();
        rest.onlyDate = "20261024";
        WeatherService service = service(rest, "2026-10-24T14:58:00Z");
        service.getHourlyWeather(60, 127, "서울시청", 0);
        rest.unavailable = true;
        ReflectionTestUtils.setField(service, "clock", utcClock("2026-10-24T14:59:00Z"));
        assertThat(service.getHourlyWeather(60, 127, "서울시청", 0).provenance().fallback()).isTrue();

        ReflectionTestUtils.setField(service, "clock", utcClock("2026-10-24T15:01:00Z"));
        assertThatThrownBy(() -> service.getHourlyWeather(60, 127, "서울시청", 0))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("최근 자료도 없습니다");
        assertThatThrownBy(() -> service.getDailyWeather(60, 127, "서울시청", 0))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("최근 자료도 없습니다");
    }

    private WeatherService service(ForecastRestTemplate rest, String instant) {
        WeatherSafetyService safety = mock(WeatherSafetyService.class);
        when(safety.getSafetyInsight(anyString(), anyInt()))
                .thenReturn(new SafetyInsightDto(-1, "-", -1, "-", "-"));
        WeatherService service = new WeatherService(rest, safety,
                new ExternalApiGuard(new SimpleMeterRegistry(), Runnable::run, Duration.ofSeconds(1)));
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://weather.example.test");
        ReflectionTestUtils.setField(service, "clock", utcClock(instant));
        return service;
    }

    private Clock utcClock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }

    private static final class ForecastRestTemplate extends RestTemplate {
        private URI lastRequest;
        private int requests;
        private boolean unavailable;
        private String onlyDate;

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getForObject(URI url, Class<T> responseType) {
            lastRequest = url;
            requests++;
            if (unavailable) throw new IllegalStateException("fixture outage");
            JSONArray items = new JSONArray();
            for (int offset = 0; offset <= 2; offset++) {
                String date = LocalDate.of(2026, 10, 24).plusDays(offset)
                        .format(DateTimeFormatter.BASIC_ISO_DATE);
                if (onlyDate != null && !onlyDate.equals(date)) continue;
                for (String time : List.of("0900", "1500", "2100")) {
                    for (List<String> field : List.of(
                            List.of("TMP", "18"), List.of("POP", "10"), List.of("PTY", "0"),
                            List.of("REH", "50"), List.of("WSD", "2.0"), List.of("SKY", "3")
                    )) {
                        items.put(new JSONObject().put("fcstDate", date).put("fcstTime", time)
                                .put("category", field.get(0)).put("fcstValue", field.get(1)));
                    }
                }
            }
            return (T) new JSONObject().put("response", new JSONObject()
                    .put("header", new JSONObject().put("resultCode", "00"))
                    .put("body", new JSONObject().put("items", new JSONObject().put("item", items))))
                    .toString();
        }
    }
}

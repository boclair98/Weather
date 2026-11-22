package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.DailyWeatherDto;
import com.example.WebSideProject.dto.SafetyInsightDto;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AirQualityServiceTest {

    @Test
    void acceptsEncodingKeyWithoutDoubleEncodingAndAddsPmValues() {
        RoutingRestTemplate restTemplate = new RoutingRestTemplate();
        WeatherService service = service(restTemplate);

        DailyWeatherDto daily = service.getDailyWeather(61, 125, "서울 강남구 강남역", 0);

        assertThat(restTemplate.airQualityUri().getRawQuery())
                .contains("serviceKey=abc%2Bdef%2Fghi%3D%3D", "ver=1.3")
                .doesNotContain("%252B", "%252F", "%253D");
        assertThat(daily.morning().getPm10Value()).isEqualTo("31");
        assertThat(daily.morning().getPm25Value()).isEqualTo("14");
        assertThat(daily.morning().getAirQualityStation()).isEqualTo("강남구");
        assertThat(daily.morning().getPm10Grade()).isEqualTo("2");
        assertThat(daily.morning().getPm25Grade()).isEqualTo("1");
        assertThat(daily.morning().getAirQualityObservedAt()).isEqualTo("2026-10-25T12:00+09:00");
        assertThat(daily.morning().getAirQualityStationBasis()).isEqualTo("LOCATION_MATCH");
    }

    @Test
    void neverUsesTwentyFourHourGradesForHourlyMeasurements() {
        JSONObject item = stationItem();
        item.remove("pm10Grade1h");
        item.remove("pm25Grade1h");
        var weather = service(new RoutingRestTemplate(item)).getDailyWeather(61, 125, "서울 강남구", 0).morning();
        assertThat(weather.getPm10Value()).isEqualTo("31");
        assertThat(weather.getPm10Grade()).isEmpty();
        assertThat(weather.getPm25Grade()).isEmpty();
        assertThat(weather.getAirQualitySummary()).doesNotContain("좋음");
    }

    @Test
    void invalidOrFlaggedValuesCannotAdvertiseAGoodGrade() {
        for (String invalid : List.of("-", "", "-1", "NaN", "Infinity")) {
            JSONObject item = stationItem().put("pm10Value", invalid).put("pm25Flag", "장비점검");
            var weather = service(new RoutingRestTemplate(item)).getDailyWeather(61, 125, "서울 강남구", 0).morning();
            assertThat(weather.getPm10Value()).isEqualTo("-");
            assertThat(weather.getPm10Grade()).isEmpty();
            assertThat(weather.getPm25Value()).isEqualTo("-");
            assertThat(weather.getPm25Grade()).isEmpty();
        }
    }

    @Test
    void preservesMeasuredZeroAndLabelsAnUnmatchedStationAsRegionalReference() {
        JSONObject item = stationItem().put("pm10Value", "0").put("pm10Grade1h", "1").put("dataTime", "invalid");
        var weather = service(new RoutingRestTemplate(item)).getDailyWeather(61, 125, "서울 송파구", 0).morning();
        assertThat(weather.getPm10Value()).isEqualTo("0");
        assertThat(weather.getPm10Grade()).isEqualTo("1");
        assertThat(weather.getAirQualityStationBasis()).isEqualTo("REGIONAL_REFERENCE");
        assertThat(weather.getAirQualityObservedAt()).isEqualTo("-");
    }

    private WeatherService service(RoutingRestTemplate restTemplate) {
        WeatherSafetyService safetyService = mock(WeatherSafetyService.class);
        when(safetyService.getSafetyInsight(anyString(), anyInt())).thenReturn(SafetyInsightDto.empty());
        WeatherService service = new WeatherService(
                restTemplate,
                safetyService,
                new ExternalApiGuard(
                        new SimpleMeterRegistry(), Runnable::run, java.time.Duration.ofSeconds(1)
                )
        );
        ReflectionTestUtils.setField(service, "apiKey", "weather-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://weather.example.test");
        ReflectionTestUtils.setField(service, "airQualityApiKey", "abc%2Bdef%2Fghi%3D%3D");
        ReflectionTestUtils.setField(service, "airQualityBaseUrl", "https://air.example.test");

        return service;
    }

    private static JSONObject stationItem() {
        return new JSONObject().put("stationName", "강남구").put("dataTime", "2026-10-25 12:00")
                .put("pm10Value", "31").put("pm10Grade", "1").put("pm10Grade1h", "2")
                .put("pm25Value", "14").put("pm25Grade", "2").put("pm25Grade1h", "1");
    }

    private static final class RoutingRestTemplate extends RestTemplate {
        private URI airQualityUri;
        private final JSONObject airItem;

        private RoutingRestTemplate() { this(stationItem()); }

        private RoutingRestTemplate(JSONObject airItem) { this.airItem = airItem; }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getForObject(URI url, Class<T> responseType) {
            if (url.getHost().startsWith("air.")) {
                airQualityUri = url;
                return (T) airQualityResponse();
            }
            return (T) weatherResponse();
        }

        private URI airQualityUri() {
            return airQualityUri;
        }

        private String weatherResponse() {
            JSONArray items = new JSONArray();
            String date = LocalDate.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.BASIC_ISO_DATE);
            for (String time : List.of("0900", "1500", "2100")) {
                add(items, date, time, "TMP", "22");
                add(items, date, time, "POP", "10");
                add(items, date, time, "PTY", "0");
                add(items, date, time, "REH", "55");
                add(items, date, time, "WSD", "1.5");
                add(items, date, time, "SKY", "1");
            }
            return new JSONObject().put("response", new JSONObject()
                    .put("header", new JSONObject().put("resultCode", "00"))
                    .put("body", new JSONObject()
                            .put("items", new JSONObject().put("item", items)))).toString();
        }

        private String airQualityResponse() {
            JSONArray items = new JSONArray().put(airItem);
            return new JSONObject().put("response", new JSONObject()
                    .put("header", new JSONObject().put("resultCode", "00"))
                    .put("body", new JSONObject().put("items", items))).toString();
        }

        private void add(JSONArray items, String date, String time, String category, String value) {
            items.put(new JSONObject()
                    .put("fcstDate", date).put("fcstTime", time)
                    .put("category", category).put("fcstValue", value));
        }
    }
}

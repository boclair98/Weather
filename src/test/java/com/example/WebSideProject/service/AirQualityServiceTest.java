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

        DailyWeatherDto daily = service.getDailyWeather(61, 125, "서울 강남구 강남역", 0);

        assertThat(restTemplate.airQualityUri().getRawQuery())
                .contains("serviceKey=abc%2Bdef%2Fghi%3D%3D")
                .doesNotContain("%252B", "%252F", "%253D");
        assertThat(daily.morning().getPm10Value()).isEqualTo("31");
        assertThat(daily.morning().getPm25Value()).isEqualTo("14");
        assertThat(daily.morning().getAirQualityStation()).isEqualTo("강남구");
    }

    private static final class RoutingRestTemplate extends RestTemplate {
        private URI airQualityUri;

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
            String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
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
            JSONArray items = new JSONArray().put(new JSONObject()
                    .put("stationName", "강남구")
                    .put("pm10Value", "31")
                    .put("pm10Grade", "2")
                    .put("pm25Value", "14")
                    .put("pm25Grade", "1"));
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

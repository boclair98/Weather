package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.DailyWeatherDto;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WeatherServicePlannerTest {

    @Test
    void buildsThreeDaysFromOneForecastRequest() {
        CountingRestTemplate restTemplate = new CountingRestTemplate(forecastResponse());
        WeatherService service = new WeatherService(restTemplate, mock(WeatherSafetyService.class));
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://weather.example.test");

        List<DailyWeatherDto> forecasts = service.getPlannerDailyWeatherList(60, 127, "서울시청");

        assertThat(forecasts).hasSize(3);
        assertThat(forecasts).extracting(DailyWeatherDto::dayLabel)
                .containsExactly("오늘", "내일", "모레");
        assertThat(restTemplate.requestCount()).isEqualTo(1);
    }

    private String forecastResponse() {
        JSONArray items = new JSONArray();
        for (int offset = 0; offset <= 2; offset++) {
            String date = LocalDate.now().plusDays(offset).format(DateTimeFormatter.BASIC_ISO_DATE);
            for (String time : List.of("0900", "1500", "2100")) {
                items.put(item(date, time, "SKY", "1"));
                items.put(item(date, time, "PTY", "0"));
                items.put(item(date, time, "TMP", String.valueOf(20 + offset)));
                items.put(item(date, time, "POP", "10"));
                items.put(item(date, time, "REH", "55"));
                items.put(item(date, time, "WSD", "2.1"));
            }
        }
        return new JSONObject()
                .put("response", new JSONObject()
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
}

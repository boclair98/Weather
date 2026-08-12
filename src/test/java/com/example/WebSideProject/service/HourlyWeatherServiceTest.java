package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.HourlyWeatherDto;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class HourlyWeatherServiceTest {

    @Test
    void exposesHourlyWeatherAndRainWindowAsPrimaryInformation() {
        WeatherService service = service(forecastResponse());

        HourlyWeatherDto hourly = service.getHourlyWeather(60, 127, "서울시청", 0);

        assertThat(hourly.hours()).hasSize(3);
        assertThat(hourly.minimumTemperature()).isEqualTo(18);
        assertThat(hourly.maximumTemperature()).isEqualTo(21);
        assertThat(hourly.maximumPrecipitationProbability()).isEqualTo(80);
        assertThat(hourly.precipitationSummary()).isEqualTo("10:00~12:00 비 가능성");
        assertThat(hourly.hours().get(1).humidity()).isEqualTo(82);
        assertThat(hourly.provenance().qualityStatus()).isEqualTo("VERIFIED");
        assertThat(hourly.provenance().completenessPercent()).isEqualTo(100);
    }

    @Test
    void rejectsDatesOutsideTheThreeDayForecast() {
        WeatherService service = service(forecastResponse());

        assertThatThrownBy(() -> service.getHourlyWeather(60, 127, "서울시청", 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0부터 2");
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
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        addHour(items, date, "0900", "18", "20", "0", "55", "1.2", "1");
        addHour(items, date, "1000", "19", "80", "1", "82", "3.4", "4");
        addHour(items, date, "1100", "21", "70", "0", "76", "2.0", "3");
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
            String humidity,
            String wind,
            String sky
    ) {
        for (List<String> field : List.of(
                List.of("TMP", temperature), List.of("POP", pop), List.of("PTY", pty),
                List.of("REH", humidity), List.of("WSD", wind), List.of("SKY", sky)
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

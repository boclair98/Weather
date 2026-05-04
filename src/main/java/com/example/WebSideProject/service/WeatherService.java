package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.WeatherDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public WeatherDto getWeather(int nx, int ny) {
        ForecastBase forecastBase = getForecastBase();
        String targetDate = getMorningForecastDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String targetTime = "0900";
        String encodedApiKey = UriUtils.encode(apiKey, StandardCharsets.UTF_8);

        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/getVilageFcst")
                .queryParam("serviceKey", encodedApiKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 100)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", forecastBase.date())
                .queryParam("base_time", forecastBase.time())
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .build(true)
                .toUri();

        log.debug("기상청 API 요청: {}", uri);

        try {
            String response = restTemplate.getForObject(uri, String.class);
            log.debug("기상청 API 응답: {}", response);
            return parseWeatherResponse(response, targetDate, targetTime);
        } catch (Exception e) {
            log.error("기상청 API 호출 실패", e);
            throw new RuntimeException("날씨 정보를 가져오는데 실패했습니다.", e);
        }
    }

    private WeatherDto parseWeatherResponse(String response, String targetDate, String targetTime) {
        JSONObject json = new JSONObject(response);
        JSONArray items = json
                .getJSONObject("response")
                .getJSONObject("body")
                .getJSONObject("items")
                .getJSONArray("item");

        Map<String, String> weatherMap = new HashMap<>();
        Map<String, String> dailyMap = new HashMap<>();
        Map<String, ForecastValue> morningMap = new HashMap<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            String category = item.getString("category");
            String fcstValue = item.getString("fcstValue");

            if (!item.getString("fcstDate").equals(targetDate)) {
                continue;
            }

            if (!dailyMap.containsKey(category)) {
                dailyMap.put(category, fcstValue);
            }

            if (item.getString("fcstTime").equals(targetTime) && !weatherMap.containsKey(category)) {
                weatherMap.put(category, fcstValue);
            }

            int morningDistance = getMorningDistance(item.getString("fcstTime"));
            if (morningDistance >= 0) {
                morningMap.merge(
                        category,
                        new ForecastValue(fcstValue, morningDistance),
                        (current, candidate) -> candidate.distance() < current.distance() ? candidate : current
                );
            }
        }

        return WeatherDto.builder()
                .date(targetDate)
                .time(targetTime)
                .sky(getForecastValue(weatherMap, morningMap, dailyMap, "SKY", "1"))
                .pty(getForecastValue(weatherMap, morningMap, dailyMap, "PTY", "0"))
                .tmp(getForecastValue(weatherMap, morningMap, dailyMap, "TMP", "-"))
                .tmn(weatherMap.getOrDefault("TMN", dailyMap.getOrDefault("TMN", "-")))
                .tmx(weatherMap.getOrDefault("TMX", dailyMap.getOrDefault("TMX", "-")))
                .pop(getForecastValue(weatherMap, morningMap, dailyMap, "POP", "0"))
                .reh(getForecastValue(weatherMap, morningMap, dailyMap, "REH", "-"))
                .wsd(getForecastValue(weatherMap, morningMap, dailyMap, "WSD", "-"))
                .build();
    }

    private String getForecastValue(
            Map<String, String> exactMap,
            Map<String, ForecastValue> morningMap,
            Map<String, String> dailyMap,
            String category,
            String defaultValue
    ) {
        if (exactMap.containsKey(category)) {
            return exactMap.get(category);
        }
        if (morningMap.containsKey(category)) {
            return morningMap.get(category).value();
        }
        return dailyMap.getOrDefault(category, defaultValue);
    }

    private int getMorningDistance(String fcstTime) {
        int hour = Integer.parseInt(fcstTime.substring(0, 2));
        if (hour < 6 || hour > 12) {
            return -1;
        }

        return Math.abs(hour - 9);
    }

    private ForecastBase getForecastBase() {
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();
        int hour = time.getHour();

        if (hour < 2 || (hour == 2 && time.getMinute() < 10)) {
            date = date.minusDays(1);
            return new ForecastBase(date.format(DateTimeFormatter.ofPattern("yyyyMMdd")), "2300");
        }

        int[] baseTimes = {2, 5, 8, 11, 14, 17, 20, 23};
        int selected = 2;
        for (int t : baseTimes) {
            if (hour >= t) selected = t;
        }

        return new ForecastBase(
                date.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                String.format("%02d00", selected)
        );
    }

    private LocalDate getMorningForecastDate() {
        return LocalTime.now().getHour() >= 18 ? LocalDate.now().plusDays(1) : LocalDate.now();
    }

    private record ForecastBase(String date, String time) {
    }

    private record ForecastValue(String value, int distance) {
    }
}

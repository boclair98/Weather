package com.example.WebSideProject.service;

import com.example.WebSideProject.Enum.WeatherPeriod;
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
        return getWeather(nx, ny, WeatherPeriod.MORNING);
    }

    public WeatherDto getWeather(int nx, int ny, WeatherPeriod period) {
        ForecastBase forecastBase = getForecastBase();
        String targetDate = getForecastDate(period).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String targetTime = period.getTargetTime();
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
            return parseWeatherResponse(response, targetDate, period);
        } catch (Exception e) {
            log.error("기상청 API 호출 실패", e);
            throw new RuntimeException("날씨 정보를 가져오는데 실패했습니다.", e);
        }
    }

    private WeatherDto parseWeatherResponse(String response, String targetDate, WeatherPeriod period) {
        String targetTime = period.getTargetTime();
        JSONObject json = new JSONObject(response);
        JSONArray items = json
                .getJSONObject("response")
                .getJSONObject("body")
                .getJSONObject("items")
                .getJSONArray("item");

        Map<String, String> weatherMap = new HashMap<>();
        Map<String, String> dailyMap = new HashMap<>();
        Map<String, ForecastValue> nearbyMap = new HashMap<>();
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

            int nearbyDistance = getNearbyDistance(item.getString("fcstTime"), period.getTargetHour());
            if (nearbyDistance >= 0) {
                nearbyMap.merge(
                        category,
                        new ForecastValue(fcstValue, nearbyDistance),
                        (current, candidate) -> candidate.distance() < current.distance() ? candidate : current
                );
            }
        }

        return WeatherDto.builder()
                .date(targetDate)
                .time(targetTime)
                .periodLabel(period.getLabel())
                .sky(getForecastValue(weatherMap, nearbyMap, dailyMap, "SKY", "1"))
                .pty(getForecastValue(weatherMap, nearbyMap, dailyMap, "PTY", "0"))
                .tmp(getForecastValue(weatherMap, nearbyMap, dailyMap, "TMP", "-"))
                .tmn(weatherMap.getOrDefault("TMN", dailyMap.getOrDefault("TMN", "-")))
                .tmx(weatherMap.getOrDefault("TMX", dailyMap.getOrDefault("TMX", "-")))
                .pop(getForecastValue(weatherMap, nearbyMap, dailyMap, "POP", "0"))
                .reh(getForecastValue(weatherMap, nearbyMap, dailyMap, "REH", "-"))
                .wsd(getForecastValue(weatherMap, nearbyMap, dailyMap, "WSD", "-"))
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

    private int getNearbyDistance(String fcstTime, int targetHour) {
        int hour = Integer.parseInt(fcstTime.substring(0, 2));
        int distance = Math.abs(hour - targetHour);
        if (distance > 3) {
            return -1;
        }

        return distance;
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

    private LocalDate getForecastDate(WeatherPeriod period) {
        return LocalDate.now();
    }

    private record ForecastBase(String date, String time) {
    }

    private record ForecastValue(String value, int distance) {
    }
}

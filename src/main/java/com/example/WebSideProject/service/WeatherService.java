package com.example.WebSideProject.service;

import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.dto.WeatherDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
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
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.base-url}")
    private String baseUrl;

    @Value("${air-quality.api.key:}")
    private String airQualityApiKey;

    @Value("${air-quality.api.base-url:https://apis.data.go.kr/B552584/ArpltnInforInqireSvc}")
    private String airQualityBaseUrl;

    private final RestTemplate restTemplate;

    public WeatherDto getWeather(int nx, int ny) {
        return getWeather(nx, ny, WeatherPeriod.MORNING);
    }

    public WeatherDto getWeather(int nx, int ny, WeatherPeriod period) {
        return getWeather(nx, ny, period, null);
    }

    @Cacheable(
            cacheNames = "weather",
            key = "#nx + ':' + #ny + ':' + #period.name() + ':' + (#locationName == null ? '' : #locationName)",
            sync = true
    )
    public WeatherDto getWeather(int nx, int ny, WeatherPeriod period, String locationName) {
        ForecastBase forecastBase = getForecastBase();
        String targetDate = getForecastDate(period).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return getWeather(nx, ny, period, locationName, forecastBase.date(), forecastBase.time(), targetDate);
    }

    public WeatherDto getWeatherForBase(
            int nx,
            int ny,
            WeatherPeriod period,
            String locationName,
            String baseDate,
            String baseTime
    ) {
        validateForecastBase(baseDate, baseTime);
        return getWeather(nx, ny, period, locationName, baseDate, baseTime, baseDate);
    }

    private WeatherDto getWeather(
            int nx,
            int ny,
            WeatherPeriod period,
            String locationName,
            String baseDate,
            String baseTime,
            String targetDate
    ) {
        String targetTime = period.getTargetTime();
        String encodedApiKey = UriUtils.encode(apiKey, StandardCharsets.UTF_8);

        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/getVilageFcst")
                .queryParam("serviceKey", encodedApiKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 1000)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .build(true)
                .toUri();

        log.debug(
                "기상청 API 요청: nx={}, ny={}, baseDate={}, baseTime={}, targetDate={}",
                nx, ny, baseDate, baseTime, targetDate
        );

        try {
            String response = restTemplate.getForObject(uri, String.class);
            WeatherDto weather = parseWeatherResponse(response, targetDate, period);
            return enrichAirQuality(weather, locationName);
        } catch (Exception e) {
            log.error("기상청 API 호출 실패", e);
            throw new RuntimeException("날씨 정보를 가져오는데 실패했습니다.", e);
        }
    }

    private void validateForecastBase(String baseDate, String baseTime) {
        if (baseDate == null || !baseDate.matches("\\d{8}")) {
            throw new IllegalArgumentException("baseDate는 yyyyMMdd 형식이어야 합니다.");
        }
        if (baseTime == null || !baseTime.matches("\\d{4}")) {
            throw new IllegalArgumentException("baseTime은 HHmm 형식이어야 합니다.");
        }

        int hour = Integer.parseInt(baseTime.substring(0, 2));
        int minute = Integer.parseInt(baseTime.substring(2, 4));
        if (minute != 0 || !List.of(2, 5, 8, 11, 14, 17, 20, 23).contains(hour)) {
            throw new IllegalArgumentException("baseTime은 0200, 0500, 0800, 1100, 1400, 1700, 2000, 2300 중 하나여야 합니다.");
        }
    }

    private WeatherDto enrichAirQuality(WeatherDto weather, String locationName) {
        if (airQualityApiKey == null || airQualityApiKey.isBlank()) {
            log.debug("AIR_QUALITY_API_KEY가 없어 대기질 조회를 건너뜁니다.");
            return weather;
        }

        try {
            AirQualityInfo airQualityInfo = getAirQuality(locationName);
            return weather.toBuilder()
                    .pm10Value(airQualityInfo.pm10Value())
                    .pm10Grade(airQualityInfo.pm10Grade())
                    .pm25Value(airQualityInfo.pm25Value())
                    .pm25Grade(airQualityInfo.pm25Grade())
                    .airQualityStation(airQualityInfo.stationName())
                    .build();
        } catch (Exception e) {
            log.warn(
                    "미세먼지 API 호출 실패. 날씨 정보만 반환합니다. locationName={}, reason={}",
                    locationName,
                    e.getMessage()
            );
            return weather;
        }
    }

    private AirQualityInfo getAirQuality(String locationName) {
        String sidoName = extractSidoName(locationName);
        String encodedApiKey = UriUtils.encode(airQualityApiKey, StandardCharsets.UTF_8);

        URI uri = UriComponentsBuilder.fromHttpUrl(airQualityBaseUrl + "/getCtprvnRltmMesureDnsty")
                .queryParam("serviceKey", encodedApiKey)
                .queryParam("returnType", "json")
                .queryParam("numOfRows", 100)
                .queryParam("pageNo", 1)
                .queryParam("sidoName", UriUtils.encode(sidoName, StandardCharsets.UTF_8))
                .queryParam("ver", "1.0")
                .build(true)
                .toUri();

        log.debug("미세먼지 API 요청: sidoName={}", sidoName);

        String response = restTemplate.getForObject(uri, String.class);
        return parseAirQualityResponse(response, locationName);
    }

    private AirQualityInfo parseAirQualityResponse(String response, String locationName) {
        JSONObject json = new JSONObject(response);
        JSONArray items = json
                .getJSONObject("response")
                .getJSONObject("body")
                .getJSONArray("items");

        if (items.length() == 0) {
            return AirQualityInfo.empty();
        }

        JSONObject selected = items.getJSONObject(0);
        String normalizedLocationName = locationName == null ? "" : locationName.replace(" ", "");
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            String stationName = item.optString("stationName", "");
            if (!stationName.isBlank() && normalizedLocationName.contains(stationName.replace(" ", ""))) {
                selected = item;
                break;
            }
        }

        return new AirQualityInfo(
                selected.optString("stationName", "-"),
                selected.optString("pm10Value", "-"),
                selected.optString("pm10Grade", ""),
                selected.optString("pm25Value", "-"),
                selected.optString("pm25Grade", "")
        );
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

    private String extractSidoName(String locationName) {
        if (locationName == null || locationName.isBlank()) {
            return "서울";
        }
        if (locationName.contains("서울")) return "서울";
        if (locationName.contains("부산")) return "부산";
        if (locationName.contains("대구")) return "대구";
        if (locationName.contains("인천")) return "인천";
        if (locationName.contains("광주")) return "광주";
        if (locationName.contains("대전")) return "대전";
        if (locationName.contains("울산")) return "울산";
        if (locationName.contains("세종")) return "세종";
        if (locationName.contains("경기")) return "경기";
        if (locationName.contains("강원")) return "강원";
        if (locationName.contains("충북")) return "충북";
        if (locationName.contains("충남")) return "충남";
        if (locationName.contains("전북")) return "전북";
        if (locationName.contains("전남")) return "전남";
        if (locationName.contains("경북")) return "경북";
        if (locationName.contains("경남")) return "경남";
        if (locationName.contains("제주")) return "제주";
        return "서울";
    }

    private record ForecastBase(String date, String time) {
    }

    private record ForecastValue(String value, int distance) {
    }

    private record AirQualityInfo(
            String stationName,
            String pm10Value,
            String pm10Grade,
            String pm25Value,
            String pm25Grade
    ) {
        private static AirQualityInfo empty() {
            return new AirQualityInfo("-", "-", "", "-", "");
        }
    }
}

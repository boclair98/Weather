package com.example.WebSideProject.service;

import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.dto.DailyWeatherDto;
import com.example.WebSideProject.dto.SafetyInsightDto;
import com.example.WebSideProject.dto.WeatherDto;
import com.example.WebSideProject.dto.ForecastProvenanceDto;
import com.example.WebSideProject.dto.WeatherDecisionDto;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private static final String KMA_SOURCE_NAME = "기상청 단기예보";
    private static final String KMA_SOURCE_URL = "https://www.weather.go.kr/w/weather/forecast/short-term.do";
    private static final Duration FALLBACK_MAX_AGE = Duration.ofHours(2);

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.base-url}")
    private String baseUrl;

    @Value("${air-quality.api.key:}")
    private String airQualityApiKey;

    @Value("${air-quality.api.base-url:https://apis.data.go.kr/B552584/ArpltnInforInqireSvc}")
    private String airQualityBaseUrl;

    private final RestTemplate restTemplate;
    private final WeatherSafetyService weatherSafetyService;
    private final ExternalApiGuard externalApiGuard;
    private final Cache<String, ForecastPayload> latestForecastSnapshots = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(FALLBACK_MAX_AGE)
            .build();

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
        validateGrid(nx, ny);
        ForecastBase forecastBase = getForecastBase();
        String targetDate = getForecastDate(period).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return getWeather(nx, ny, period, locationName, forecastBase.date(), forecastBase.time(), targetDate);
    }

    public DailyWeatherDto getDailyWeather(int nx, int ny, String locationName) {
        return getDailyWeather(nx, ny, locationName, 0);
    }

    @Cacheable(
            cacheNames = "weather",
            key = "'daily:' + #nx + ':' + #ny + ':' + #dayOffset + ':' + (#locationName == null ? '' : #locationName)",
            sync = true
    )
    public DailyWeatherDto getDailyWeather(int nx, int ny, String locationName, int dayOffset) {
        validateGrid(nx, ny);
        if (dayOffset < 0 || dayOffset > 2) {
            throw new IllegalArgumentException("dayOffset은 0부터 2까지만 지원합니다.");
        }
        return buildDailyWeather(nx, ny, locationName, dayOffset, true);
    }

    @Cacheable(
            cacheNames = "plannerSource",
            key = "'planner-three-day:' + #nx + ':' + #ny + ':' + (#locationName == null ? '' : #locationName)",
            sync = true
    )
    public List<DailyWeatherDto> getPlannerDailyWeatherList(int nx, int ny, String locationName) {
        validateGrid(nx, ny);
        ForecastBase forecastBase = getDailyForecastBase();

        // A single village-forecast response already contains today through the day after tomorrow.
        // Reusing it avoids three network round trips and keeps cold planner requests below gateways.
        ForecastPayload response = requestForecast(nx, ny, forecastBase.date(), forecastBase.time(), null);
        List<DailyWeatherDto> forecasts = new ArrayList<>(3);
        for (int dayOffset = 0; dayOffset <= 2; dayOffset++) {
            String targetDate = LocalDate.now().plusDays(dayOffset)
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            WeatherDto morning = parseWeatherResponse(response, targetDate, WeatherPeriod.MORNING);
            WeatherDto afternoon = parseWeatherResponse(response, targetDate, WeatherPeriod.AFTERNOON);
            WeatherDto evening = parseWeatherResponse(response, targetDate, WeatherPeriod.EVENING);
            String dayLabel = switch (dayOffset) {
                case 0 -> "오늘";
                case 1 -> "내일";
                default -> "모레";
            };
            forecasts.add(DailyWeatherDto.from(morning, afternoon, evening, dayLabel, targetDate));
        }
        return List.copyOf(forecasts);
    }

    @Cacheable(
            cacheNames = "weather",
            key = "'decision:' + #nx + ':' + #ny + ':' + #targetDate + ':' + #targetTime + ':'"
                    + " + #flexMinutes + ':' + #durationMinutes",
            sync = true
    )
    public WeatherDecisionDto getDecisionWindow(
            int nx,
            int ny,
            String locationName,
            String targetDate,
            String targetTime,
            int flexMinutes,
            int durationMinutes
    ) {
        validateGrid(nx, ny);
        LocalDate date = parseDecisionDate(targetDate);
        LocalTime requestedTime = parseDecisionTime(targetTime);
        if (flexMinutes < 0 || flexMinutes > 180) {
            throw new IllegalArgumentException("조정 가능 시간은 0분부터 180분까지 지원합니다.");
        }
        if (durationMinutes < 30 || durationMinutes > 180 || durationMinutes % 30 != 0) {
            throw new IllegalArgumentException("외출 시간은 30분 단위로 30분부터 180분까지 지원합니다.");
        }

        ForecastBase forecastBase = getDailyForecastBase();
        ForecastPayload payload = requestForecast(
                nx, ny, forecastBase.date(), forecastBase.time(), date.format(DateTimeFormatter.BASIC_ISO_DATE)
        );
        List<HourlyForecast> hourly = parseHourlyForecasts(payload, date);
        if (hourly.isEmpty()) {
            throw new IllegalStateException("선택한 시간에 사용할 수 있는 시간별 예보가 없습니다.");
        }

        LocalDateTime requested = LocalDateTime.of(date, requestedTime);
        List<DecisionCandidate> candidates = buildDecisionCandidates(
                hourly, requested, flexMinutes, durationMinutes
        );
        if (candidates.isEmpty()) {
            throw new IllegalStateException("선택한 범위에 완전한 시간별 예보가 없습니다. 시간을 조금 뒤로 조정해주세요.");
        }

        DecisionCandidate baseline = candidates.stream()
                .min(Comparator.comparingLong(candidate -> Math.abs(candidate.shiftMinutes())))
                .orElseThrow();
        DecisionCandidate recommended = candidates.stream()
                .max(Comparator.comparingInt(DecisionCandidate::score)
                        .thenComparingLong(candidate -> -Math.abs(candidate.shiftMinutes())))
                .orElseThrow();
        int improvement = Math.max(0, recommended.score() - baseline.score());
        List<String> checklist = buildDecisionChecklist(recommended);
        String headline = buildDecisionHeadline(recommended, improvement);
        String rationale = buildDecisionRationale(baseline, recommended);

        List<WeatherDecisionDto.WindowCandidate> responseCandidates = candidates.stream()
                .sorted(Comparator.comparing(DecisionCandidate::start))
                .map(candidate -> new WeatherDecisionDto.WindowCandidate(
                        candidate.start().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                        candidate.end().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                        candidate.score(),
                        scoreLabel(candidate.score()),
                        candidate.maxPop(),
                        precipitationLabel(candidate.pty()),
                        candidate.temperature(),
                        Math.round(candidate.maxWind() * 10.0) / 10.0,
                        candidate.start().equals(recommended.start())
                ))
                .toList();

        return new WeatherDecisionDto(
                locationName == null || locationName.isBlank() ? "선택 위치" : locationName,
                date.toString(),
                requestedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                recommended.start().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                recommended.end().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                durationMinutes,
                (int) recommended.shiftMinutes(),
                baseline.score(),
                recommended.score(),
                improvement,
                headline,
                rationale,
                checklist,
                responseCandidates,
                decisionProvenance(payload, hourly)
        );
    }

    private DailyWeatherDto buildDailyWeather(
            int nx,
            int ny,
            String locationName,
            int dayOffset,
            boolean includeSafetyDetails
    ) {
        ForecastBase forecastBase = getDailyForecastBase();
        String targetDate = LocalDate.now().plusDays(dayOffset)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        ForecastPayload response = requestForecast(nx, ny, forecastBase.date(), forecastBase.time(), targetDate);

        WeatherDto morning = parseWeatherResponse(response, targetDate, WeatherPeriod.MORNING);
        WeatherDto afternoon = parseWeatherResponse(response, targetDate, WeatherPeriod.AFTERNOON);
        WeatherDto evening = parseWeatherResponse(response, targetDate, WeatherPeriod.EVENING);

        String dayLabel = switch (dayOffset) {
            case 0 -> "오늘";
            case 1 -> "내일";
            default -> "모레";
        };
        if (!includeSafetyDetails) {
            return DailyWeatherDto.from(morning, afternoon, evening, dayLabel, targetDate);
        }

        AirQualityInfo airQuality = safelyGetAirQuality(locationName);
        SafetyInsightDto safetyInsight = weatherSafetyService.getSafetyInsight(locationName, dayOffset);
        return DailyWeatherDto.from(
                applySafetyInsight(applyAirQuality(morning, airQuality), safetyInsight),
                applySafetyInsight(applyAirQuality(afternoon, airQuality), safetyInsight),
                applySafetyInsight(applyAirQuality(evening, airQuality), safetyInsight),
                dayLabel, targetDate
        );
    }

    public WeatherDto getWeatherForBase(
            int nx,
            int ny,
            WeatherPeriod period,
            String locationName,
            String baseDate,
            String baseTime
    ) {
        validateGrid(nx, ny);
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
        ForecastPayload response = requestForecast(nx, ny, baseDate, baseTime, targetDate);
        WeatherDto weather = parseWeatherResponse(response, targetDate, period);
        int dayOffset = Math.max(0, Math.min(2,
                (int) java.time.temporal.ChronoUnit.DAYS.between(
                        LocalDate.now(), LocalDate.parse(targetDate, DateTimeFormatter.BASIC_ISO_DATE))));
        SafetyInsightDto safetyInsight = weatherSafetyService.getSafetyInsight(locationName, dayOffset);
        return applySafetyInsight(enrichAirQuality(weather, locationName), safetyInsight);
    }

    private ForecastPayload requestForecast(
            int nx,
            int ny,
            String baseDate,
            String baseTime,
            String targetDate
    ) {
        String encodedApiKey = UriUtils.encode(apiKey, StandardCharsets.UTF_8);

        URI uri = UriComponentsBuilder.fromUriString(baseUrl + "/getVilageFcst")
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

        String snapshotKey = nx + ":" + ny;
        try {
            String response = externalApiGuard.execute("kma-forecast", () -> {
                String body = restTemplate.getForObject(uri, String.class);
                validateForecastResponse(body);
                return body;
            });
            ForecastPayload payload = new ForecastPayload(
                    response,
                    forecastIssuedAt(baseDate, baseTime),
                    Instant.now(),
                    false
            );
            latestForecastSnapshots.put(snapshotKey, payload);
            return payload;
        } catch (RuntimeException e) {
            ForecastPayload snapshot = latestForecastSnapshots.getIfPresent(snapshotKey);
            if (snapshot != null
                    && Duration.between(snapshot.fetchedAt(), Instant.now()).compareTo(FALLBACK_MAX_AGE) <= 0) {
                log.warn(
                        "기상청 API 장애로 마지막 정상 예보 사용: nx={}, ny={}, fetchedAt={}",
                        nx, ny, snapshot.fetchedAt()
                );
                return snapshot.asFallback();
            }
            log.error("기상청 API 호출 실패: nx={}, ny={}", nx, ny, e);
            throw new IllegalStateException(
                    "기상청 예보를 가져오지 못했고 사용 가능한 최근 자료도 없습니다.",
                    e
            );
        }
    }

    private void validateForecastResponse(String response) {
        if (response == null || response.isBlank()) {
            throw new IllegalStateException("기상청이 빈 응답을 반환했습니다.");
        }
        try {
            JSONObject responseNode = new JSONObject(response).getJSONObject("response");
            String resultCode = responseNode.getJSONObject("header").optString("resultCode", "00");
            if (!"00".equals(resultCode)) {
                String resultMessage = responseNode.getJSONObject("header")
                        .optString("resultMsg", "원천 API 오류");
                throw new IllegalStateException("기상청 응답 오류: " + resultCode + " " + resultMessage);
            }
            responseNode.getJSONObject("body").getJSONObject("items").getJSONArray("item");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("기상청 응답 형식이 계약과 다릅니다.", e);
        }
    }

    private String forecastIssuedAt(String baseDate, String baseTime) {
        return LocalDateTime.parse(
                        baseDate + baseTime,
                        DateTimeFormatter.ofPattern("yyyyMMddHHmm")
                )
                .atZone(ZoneId.of("Asia/Seoul"))
                .toOffsetDateTime()
                .toString();
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

    private void validateGrid(int nx, int ny) {
        if (nx < 1 || nx > 149 || ny < 1 || ny > 253) {
            throw new IllegalArgumentException("대한민국 예보 격자 범위의 위치만 조회할 수 있습니다.");
        }
    }

    private WeatherDto enrichAirQuality(WeatherDto weather, String locationName) {
        return applyAirQuality(weather, safelyGetAirQuality(locationName));
    }

    private AirQualityInfo safelyGetAirQuality(String locationName) {
        if (airQualityApiKey == null || airQualityApiKey.isBlank()) {
            log.debug("AIR_QUALITY_API_KEY가 없어 대기질 조회를 건너뜁니다.");
            return AirQualityInfo.empty();
        }
        try {
            return getAirQuality(locationName);
        } catch (Exception e) {
            log.warn(
                    "미세먼지 API 호출 실패. 날씨 정보만 반환합니다. locationName={}, reason={}",
                    locationName,
                    e.getMessage()
            );
            return AirQualityInfo.empty();
        }
    }

    private WeatherDto applyAirQuality(WeatherDto weather, AirQualityInfo airQualityInfo) {
        return weather.toBuilder()
                .pm10Value(airQualityInfo.pm10Value())
                .pm10Grade(airQualityInfo.pm10Grade())
                .pm25Value(airQualityInfo.pm25Value())
                .pm25Grade(airQualityInfo.pm25Grade())
                .airQualityStation(airQualityInfo.stationName())
                .build();
    }

    private WeatherDto applySafetyInsight(WeatherDto weather, SafetyInsightDto safetyInsight) {
        return weather.toBuilder()
                .uvIndex(safetyInsight.uvIndex())
                .pollenType(safetyInsight.pollenType())
                .pollenRiskLevel(safetyInsight.pollenRiskLevel())
                .weatherWarningTitle(safetyInsight.weatherWarningTitle())
                .weatherWarningDetails(safetyInsight.weatherWarningDetails())
                .build();
    }

    private AirQualityInfo getAirQuality(String locationName) {
        String sidoName = extractSidoName(locationName);
        String encodedApiKey = UriUtils.encode(airQualityApiKey, StandardCharsets.UTF_8);

        URI uri = UriComponentsBuilder.fromUriString(airQualityBaseUrl + "/getCtprvnRltmMesureDnsty")
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

    private WeatherDto parseWeatherResponse(ForecastPayload payload, String targetDate, WeatherPeriod period) {
        String targetTime = period.getTargetTime();
        JSONObject json = new JSONObject(payload.body());
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
                .dataSourceName(KMA_SOURCE_NAME)
                .dataSourceUrl(KMA_SOURCE_URL)
                .forecastIssuedAt(payload.forecastIssuedAt())
                .dataFetchedAt(payload.fetchedAt().toString())
                .fallbackData(payload.fallback())
                .sourceFieldCount(countSourceFields(weatherMap, nearbyMap, dailyMap))
                .build();
    }

    private LocalDate parseDecisionDate(String value) {
        LocalDate date;
        try {
            date = value == null || value.isBlank() ? LocalDate.now() : LocalDate.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("날짜는 yyyy-MM-dd 형식으로 입력해주세요.");
        }
        LocalDate today = LocalDate.now();
        if (date.isBefore(today) || date.isAfter(today.plusDays(2))) {
            throw new IllegalArgumentException("일정 최적화는 오늘부터 모레까지만 지원합니다.");
        }
        return date;
    }

    private LocalTime parseDecisionTime(String value) {
        try {
            String normalized = value == null || value.isBlank() ? "18:00" : value.trim();
            return LocalTime.parse(normalized, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            throw new IllegalArgumentException("시간은 HH:mm 형식으로 입력해주세요.");
        }
    }

    private List<HourlyForecast> parseHourlyForecasts(ForecastPayload payload, LocalDate targetDate) {
        JSONArray items = new JSONObject(payload.body())
                .getJSONObject("response")
                .getJSONObject("body")
                .getJSONObject("items")
                .getJSONArray("item");
        String dateKey = targetDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        Map<String, Map<String, String>> valuesByTime = new HashMap<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            if (!dateKey.equals(item.optString("fcstDate"))) continue;
            String forecastTime = item.optString("fcstTime");
            if (forecastTime.length() != 4) continue;
            valuesByTime.computeIfAbsent(forecastTime, ignored -> new HashMap<>())
                    .putIfAbsent(item.optString("category"), item.optString("fcstValue"));
        }

        return valuesByTime.entrySet().stream()
                .filter(entry -> entry.getValue().containsKey("TMP"))
                .map(entry -> {
                    Map<String, String> values = entry.getValue();
                    LocalTime time = LocalTime.parse(entry.getKey(), DateTimeFormatter.ofPattern("HHmm"));
                    return new HourlyForecast(
                            LocalDateTime.of(targetDate, time),
                            safeParseInt(values.get("POP"), 0),
                            values.getOrDefault("PTY", "0"),
                            safeParseInt(values.get("TMP"), 20),
                            safeParseInt(values.get("REH"), 50),
                            safeParseDouble(values.get("WSD"), 0.0),
                            (int) List.of("TMP", "POP", "PTY", "REH", "WSD", "SKY").stream()
                                    .filter(values::containsKey)
                                    .count()
                    );
                })
                .sorted(Comparator.comparing(HourlyForecast::time))
                .toList();
    }

    private List<DecisionCandidate> buildDecisionCandidates(
            List<HourlyForecast> hourly,
            LocalDateTime requested,
            int flexMinutes,
            int durationMinutes
    ) {
        Map<LocalDateTime, HourlyForecast> byTime = new HashMap<>();
        hourly.forEach(point -> byTime.put(point.time(), point));
        int requiredPoints = Math.max(1, (int) Math.ceil(durationMinutes / 60.0));
        List<DecisionCandidate> candidates = new ArrayList<>();

        for (HourlyForecast start : hourly) {
            long shift = Duration.between(requested, start.time()).toMinutes();
            if (Math.abs(shift) > flexMinutes) continue;
            List<HourlyForecast> window = new ArrayList<>(requiredPoints);
            for (int hour = 0; hour < requiredPoints; hour++) {
                HourlyForecast point = byTime.get(start.time().plusHours(hour));
                if (point != null) window.add(point);
            }
            if (window.size() != requiredPoints) continue;

            int maxPop = window.stream().mapToInt(HourlyForecast::pop).max().orElse(0);
            String pty = window.stream().map(HourlyForecast::pty)
                    .filter(code -> !"0".equals(code))
                    .findFirst().orElse("0");
            int temperature = (int) Math.round(window.stream()
                    .mapToInt(HourlyForecast::temperature).average().orElse(20));
            int humidity = window.stream().mapToInt(HourlyForecast::humidity).max().orElse(50);
            double maxWind = window.stream().mapToDouble(HourlyForecast::wind).max().orElse(0.0);
            int score = decisionScore(maxPop, pty, temperature, humidity, maxWind);
            candidates.add(new DecisionCandidate(
                    start.time(), start.time().plusMinutes(durationMinutes), shift,
                    score, maxPop, pty, temperature, humidity, maxWind
            ));
        }
        return candidates;
    }

    private int decisionScore(int pop, String pty, int temperature, int humidity, double wind) {
        int penalty = (int) Math.round(pop * 0.5);
        if (!"0".equals(pty)) penalty += 28;
        if (wind > 6.0) penalty += Math.min(20, (int) Math.round((wind - 6.0) * 4));
        if (temperature >= 30) penalty += Math.min(18, (temperature - 29) * 4);
        if (temperature <= 5) penalty += Math.min(18, (6 - temperature) * 3);
        if (humidity >= 85) penalty += 6;
        return Math.max(0, Math.min(100, 100 - penalty));
    }

    private String scoreLabel(int score) {
        if (score >= 85) return "쾌적";
        if (score >= 70) return "무난";
        if (score >= 50) return "준비 필요";
        return "시간 조정 권장";
    }

    private String precipitationLabel(String pty) {
        return switch (pty) {
            case "1" -> "비";
            case "2" -> "비·눈";
            case "3" -> "눈";
            case "4" -> "소나기";
            default -> "없음";
        };
    }

    private String buildDecisionHeadline(DecisionCandidate recommended, int improvement) {
        String time = recommended.start().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        if (improvement >= 5) {
            return time + "에 출발하면 날씨 부담을 " + improvement + "점 줄일 수 있어요.";
        }
        return time + " 출발이 가장 안정적이에요. 원래 일정도 큰 차이는 없습니다.";
    }

    private String buildDecisionRationale(DecisionCandidate baseline, DecisionCandidate recommended) {
        List<String> reasons = new ArrayList<>();
        if (recommended.maxPop() < baseline.maxPop()) {
            reasons.add("강수확률 " + baseline.maxPop() + "% → " + recommended.maxPop() + "%");
        }
        if (recommended.maxWind() + 0.5 < baseline.maxWind()) {
            reasons.add(String.format("바람 %.1fm/s → %.1fm/s", baseline.maxWind(), recommended.maxWind()));
        }
        if (!"0".equals(baseline.pty()) && "0".equals(recommended.pty())) {
            reasons.add("비·눈 시간대 회피");
        }
        if (reasons.isEmpty()) {
            reasons.add("강수·바람·기온을 함께 비교한 결과");
        }
        return String.join(" · ", reasons);
    }

    private List<String> buildDecisionChecklist(DecisionCandidate candidate) {
        Set<String> checklist = new LinkedHashSet<>();
        if (!"0".equals(candidate.pty()) || candidate.maxPop() >= 40) checklist.add("접이식 우산");
        if (candidate.maxWind() >= 8.0) checklist.add("바람에 날리는 소지품 고정");
        if (candidate.temperature() >= 28) checklist.add("마실 물");
        if (candidate.temperature() <= 8) checklist.add("체온 조절 겉옷");
        if (candidate.humidity() >= 85) checklist.add("통풍·속건 옷차림");
        if (checklist.isEmpty()) checklist.add("기본 외출 준비");
        return List.copyOf(checklist);
    }

    private ForecastProvenanceDto decisionProvenance(ForecastPayload payload, List<HourlyForecast> hourly) {
        int available = hourly.stream().mapToInt(HourlyForecast::sourceFieldCount).sum();
        int expected = hourly.size() * 6;
        int completeness = expected == 0 ? 0 : (int) Math.round(available * 100.0 / expected);
        long ageSeconds = Math.max(0, Duration.between(payload.fetchedAt(), Instant.now()).getSeconds());
        String freshness = payload.fallback() ? "STALE_FALLBACK"
                : ageSeconds <= 900 ? "FRESH" : ageSeconds <= 3600 ? "RECENT" : "STALE";
        String quality = payload.fallback() ? "DEGRADED"
                : completeness >= 90 ? "VERIFIED" : completeness >= 70 ? "PARTIAL" : "DEGRADED";
        return new ForecastProvenanceDto(
                KMA_SOURCE_NAME, KMA_SOURCE_URL, payload.forecastIssuedAt(), payload.fetchedAt().toString(),
                ageSeconds, freshness, quality, completeness, payload.fallback(),
                payload.fallback()
                        ? "외부 원천 장애로 마지막 정상 예보를 사용해 계산했습니다."
                        : "기상청 시간별 단기예보를 일정 주변에서 비교한 결과입니다."
        );
    }

    private int safeParseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private double safeParseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private int countSourceFields(
            Map<String, String> exactMap,
            Map<String, ForecastValue> nearbyMap,
            Map<String, String> dailyMap
    ) {
        int count = 0;
        for (String category : List.of("TMP", "POP", "REH", "WSD", "SKY", "PTY")) {
            if (exactMap.containsKey(category)
                    || nearbyMap.containsKey(category)
                    || dailyMap.containsKey(category)) {
                count++;
            }
        }
        return count;
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

    private ForecastBase getDailyForecastBase() {
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();
        if (time.isBefore(LocalTime.of(2, 10))) {
            return new ForecastBase(
                    date.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                    "2300"
            );
        }
        return new ForecastBase(date.format(DateTimeFormatter.ofPattern("yyyyMMdd")), "0200");
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

    private record ForecastPayload(
            String body,
            String forecastIssuedAt,
            Instant fetchedAt,
            boolean fallback
    ) {
        private ForecastPayload asFallback() {
            return new ForecastPayload(body, forecastIssuedAt, fetchedAt, true);
        }
    }

    private record ForecastValue(String value, int distance) {
    }

    private record HourlyForecast(
            LocalDateTime time,
            int pop,
            String pty,
            int temperature,
            int humidity,
            double wind,
            int sourceFieldCount
    ) {
    }

    private record DecisionCandidate(
            LocalDateTime start,
            LocalDateTime end,
            long shiftMinutes,
            int score,
            int maxPop,
            String pty,
            int temperature,
            int humidity,
            double maxWind
    ) {
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

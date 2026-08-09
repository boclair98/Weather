package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.SafetyInsightDto;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherSafetyService {

    private static final DateTimeFormatter API_HOUR = DateTimeFormatter.ofPattern("yyyyMMddHH");

    @Value("${weather-safety.warning.api-key:}")
    private String warningApiKey;

    @Value("${weather-safety.warning.base-url:https://apis.data.go.kr/1360000/WthrWrnInfoService}")
    private String warningBaseUrl;

    @Value("${weather-safety.uv.api-key:}")
    private String uvApiKey;

    @Value("${weather-safety.uv.base-url:https://apis.data.go.kr/1360000/LivingWthrIdxServiceV5}")
    private String uvBaseUrl;

    @Value("${weather-safety.pollen.api-key:}")
    private String pollenApiKey;

    @Value("${weather-safety.pollen.base-url:https://apis.data.go.kr/1360000/HealthWthrIdxServiceV3}")
    private String pollenBaseUrl;

    private final RestTemplate restTemplate;

    @Cacheable(
            cacheNames = "weather",
            key = "'safety:' + (#locationName == null ? '' : #locationName) + ':' + #dayOffset",
            sync = true
    )
    public SafetyInsightDto getSafetyInsight(String locationName, int dayOffset) {
        if (dayOffset < 0 || dayOffset > 2) {
            throw new IllegalArgumentException("dayOffset은 0부터 2까지만 지원합니다.");
        }

        int uvIndex = safelyGetUvIndex(locationName, dayOffset);
        PollenInsight pollen = safelyGetPollen(locationName, dayOffset);
        WarningInsight warning = dayOffset == 0
                ? safelyGetWarning(locationName)
                : WarningInsight.empty();
        return new SafetyInsightDto(
                uvIndex,
                pollen.type(),
                pollen.level(),
                warning.title(),
                warning.details()
        );
    }

    private int safelyGetUvIndex(String locationName, int dayOffset) {
        if (isBlank(uvApiKey)) return -1;
        try {
            String areaNo = areaCode(locationName);
            LocalDateTime requestedAt = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
            URI uri = publicDataUri(uvBaseUrl + "/getUVIdxV5", uvApiKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 1)
                    .queryParam("dataType", "JSON")
                    .queryParam("areaNo", areaNo)
                    .queryParam("time", requestedAt.format(API_HOUR))
                    .build(true)
                    .toUri();
            String response = restTemplate.getForObject(uri, String.class);
            return parseUvIndex(response, requestedAt.toLocalDate().plusDays(dayOffset));
        } catch (Exception e) {
            log.warn("자외선지수 조회 실패. 기본 날씨만 제공합니다. reason={}", e.getMessage());
            return -1;
        }
    }

    private PollenInsight safelyGetPollen(String locationName, int dayOffset) {
        if (isBlank(pollenApiKey)) return PollenInsight.empty();
        try {
            String endpoint = pollenEndpoint(LocalDate.now().getMonthValue());
            URI uri = publicDataUri(pollenBaseUrl + "/" + endpoint, pollenApiKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 1)
                    .queryParam("dataType", "JSON")
                    .queryParam("areaNo", areaCode(locationName))
                    .queryParam("time", LocalDateTime.now().format(API_HOUR))
                    .build(true)
                    .toUri();
            String response = restTemplate.getForObject(uri, String.class);
            return parsePollen(response, dayOffset, pollenType(endpoint));
        } catch (Exception e) {
            log.warn("꽃가루지수 조회 실패. 기본 날씨만 제공합니다. reason={}", e.getMessage());
            return PollenInsight.empty();
        }
    }

    private WarningInsight safelyGetWarning(String locationName) {
        if (isBlank(warningApiKey)) return WarningInsight.empty();
        try {
            String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            URI uri = publicDataUri(warningBaseUrl + "/getWthrWrnMsg", warningApiKey)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 1)
                    .queryParam("dataType", "JSON")
                    .queryParam("fromTmFc", today)
                    .queryParam("toTmFc", today)
                    .queryParam("stnId", 108)
                    .build(true)
                    .toUri();
            String response = restTemplate.getForObject(uri, String.class);
            return parseWarning(response, regionKeyword(locationName));
        } catch (Exception e) {
            log.warn("기상특보 조회 실패. 기본 날씨만 제공합니다. reason={}", e.getMessage());
            return WarningInsight.empty();
        }
    }

    int parseUvIndex(String response, LocalDate targetDate) {
        JSONArray items = responseItems(response);
        if (items.isEmpty()) return -1;
        JSONObject item = items.getJSONObject(0);
        LocalDateTime base = LocalDateTime.parse(item.getString("date"), API_HOUR);
        List<Integer> values = new ArrayList<>();
        for (int offset = 0; offset <= 75; offset += 3) {
            if (!base.plusHours(offset).toLocalDate().equals(targetDate)) continue;
            int value = parseInt(item.optString("h" + offset), -1);
            if (value >= 0) values.add(value);
        }
        return values.stream().mapToInt(Integer::intValue).max().orElse(-1);
    }

    PollenInsight parsePollen(String response, int dayOffset, String type) {
        JSONArray items = responseItems(response);
        if (items.isEmpty()) return PollenInsight.empty();
        JSONObject item = items.getJSONObject(0);
        String field = switch (dayOffset) {
            case 0 -> "today";
            case 1 -> "tomorrow";
            default -> "dayaftertomorrow";
        };
        return new PollenInsight(type, parseInt(item.optString(field), -1));
    }

    WarningInsight parseWarning(String response, String region) {
        JSONArray items = responseItems(response);
        if (items.isEmpty()) return WarningInsight.empty();
        String activeWarnings = items.getJSONObject(0).optString("t6", "");
        for (String rawLine : activeWarnings.split("\\R")) {
            String line = rawLine.replaceFirst("^o\\s*", "").trim();
            if (line.isBlank() || !line.contains(region)) continue;
            String title = line.contains(":") ? line.substring(0, line.indexOf(':')).trim() : "기상특보";
            return new WarningInsight(title, region + " 지역에 " + title + "가 발효 중입니다.");
        }
        return WarningInsight.empty();
    }

    private UriComponentsBuilder publicDataUri(String url, String apiKey) {
        String encodedKey = apiKey.contains("%")
                ? apiKey
                : UriUtils.encode(apiKey, StandardCharsets.UTF_8);
        return UriComponentsBuilder.fromHttpUrl(url).queryParam("serviceKey", encodedKey);
    }

    private JSONArray responseItems(String response) {
        if (response == null || response.isBlank()) return new JSONArray();
        JSONObject body = new JSONObject(response)
                .optJSONObject("response")
                .optJSONObject("body");
        if (body == null) return new JSONArray();
        JSONObject items = body.optJSONObject("items");
        return items == null ? new JSONArray() : items.optJSONArray("item", new JSONArray());
    }

    private String pollenEndpoint(int month) {
        if (month >= 3 && month <= 5) return "getOakPollenRiskIdxV3";
        if (month >= 5 && month <= 7) return "getPinePollenRiskIdxV3";
        return "getWeedsPollenRiskndxV3";
    }

    private String pollenType(String endpoint) {
        if (endpoint.contains("Oak")) return "참나무";
        if (endpoint.contains("Pine")) return "소나무";
        return "잡초류";
    }

    private String areaCode(String locationName) {
        return switch (regionKeyword(locationName)) {
            case "부산" -> "2600000000";
            case "대구" -> "2700000000";
            case "인천" -> "2800000000";
            case "광주" -> "2900000000";
            case "대전" -> "3000000000";
            case "울산" -> "3100000000";
            case "세종" -> "3600000000";
            case "경기" -> "4100000000";
            case "강원" -> "5100000000";
            case "충북" -> "4300000000";
            case "충남" -> "4400000000";
            case "전북" -> "5200000000";
            case "전남" -> "4600000000";
            case "경북" -> "4700000000";
            case "경남" -> "4800000000";
            case "제주" -> "5000000000";
            default -> "1100000000";
        };
    }

    private String regionKeyword(String locationName) {
        String value = locationName == null ? "" : locationName;
        if (value.contains("부산")) return "부산";
        if (value.contains("대구")) return "대구";
        if (value.contains("인천")) return "인천";
        if (value.contains("광주")) return "광주";
        if (value.contains("대전")) return "대전";
        if (value.contains("울산")) return "울산";
        if (value.contains("세종")) return "세종";
        if (value.contains("경기") || value.contains("수원") || value.contains("성남")) return "경기";
        if (value.contains("강원")) return "강원";
        if (value.contains("충북")) return "충북";
        if (value.contains("충남")) return "충남";
        if (value.contains("전북")) return "전북";
        if (value.contains("전남")) return "전남";
        if (value.contains("경북")) return "경북";
        if (value.contains("경남") || value.contains("창원")) return "경남";
        if (value.contains("제주")) return "제주";
        return "서울";
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    record PollenInsight(String type, int level) {
        static PollenInsight empty() {
            return new PollenInsight("-", -1);
        }
    }

    record WarningInsight(String title, String details) {
        static WarningInsight empty() {
            return new WarningInsight("-", "-");
        }
    }
}

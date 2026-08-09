package com.example.WebSideProject.service;

import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.dto.LocationDto;
import com.example.WebSideProject.dto.RouteBriefingDto;
import com.example.WebSideProject.dto.WeatherDto;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class RouteBriefingService {

    @Value("${route.kakao-mobility.rest-api-key:}")
    private String kakaoMobilityKey;

    @Value("${route.kakao-mobility.base-url:https://apis-navi.kakaomobility.com/v1}")
    private String kakaoMobilityBaseUrl;

    private final RestTemplate restTemplate;
    private final LocationService locationService;
    private final WeatherService weatherService;

    @Cacheable(
            cacheNames = "weather",
            key = "'route:' + #originQuery.trim().toLowerCase() + ':' + #destinationQuery.trim().toLowerCase() + ':' + #period.name()",
            sync = true
    )
    public RouteBriefingDto getBriefing(
            String originQuery,
            String destinationQuery,
            WeatherPeriod period
    ) {
        if (originQuery == null || originQuery.isBlank()
                || destinationQuery == null || destinationQuery.isBlank()) {
            throw new IllegalArgumentException("출발지와 목적지를 모두 입력해주세요.");
        }
        if (originQuery.trim().length() > 100 || destinationQuery.trim().length() > 100) {
            throw new IllegalArgumentException("장소명은 각각 100자 이내로 입력해주세요.");
        }
        if (kakaoMobilityKey == null || kakaoMobilityKey.isBlank()) {
            throw new IllegalStateException("경로 브리핑 API가 아직 설정되지 않았습니다.");
        }

        LocationDto.Response origin = firstLocation(originQuery, "출발지");
        LocationDto.Response destination = firstLocation(destinationQuery, "목적지");
        RouteSummary route = requestRoute(origin, destination);
        WeatherDto originWeather = weatherService.getWeather(
                origin.getNx(), origin.getNy(), period, searchableName(origin));
        WeatherDto destinationWeather = weatherService.getWeather(
                destination.getNx(), destination.getNy(), period, searchableName(destination));
        return RouteBriefingDto.from(
                origin,
                destination,
                route.distanceMeters(),
                route.durationSeconds(),
                originWeather,
                destinationWeather
        );
    }

    private LocationDto.Response firstLocation(String query, String label) {
        return locationService.search(query).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(label + " 검색 결과가 없습니다."));
    }

    private RouteSummary requestRoute(LocationDto.Response origin, LocationDto.Response destination) {
        URI uri = UriComponentsBuilder.fromUriString(kakaoMobilityBaseUrl + "/directions")
                .queryParam("origin", origin.getLongitude() + "," + origin.getLatitude())
                .queryParam("destination", destination.getLongitude() + "," + destination.getLatitude())
                .queryParam("summary", true)
                .build(true)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoMobilityKey);
        String response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        ).getBody();
        JSONObject json = new JSONObject(response == null ? "{}" : response);
        JSONArray routes = json.optJSONArray("routes");
        if (routes == null || routes.isEmpty()) {
            throw new IllegalArgumentException("선택한 두 장소 사이의 자동차 경로를 찾지 못했습니다.");
        }
        JSONObject summary = routes.getJSONObject(0).getJSONObject("summary");
        return new RouteSummary(summary.getInt("distance"), summary.getInt("duration"));
    }

    private String searchableName(LocationDto.Response location) {
        String region = location.getRegionName();
        return region == null || region.isBlank()
                ? location.getLocationName()
                : location.getLocationName() + " " + region;
    }

    private record RouteSummary(int distanceMeters, int durationSeconds) {
    }
}

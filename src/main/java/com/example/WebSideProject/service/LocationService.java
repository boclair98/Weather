package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.LocationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationService {

    private static final int KAKAO_RESULT_SIZE = 15;
    private static final List<FallbackLocation> FALLBACK_LOCATIONS = List.of(
            new FallbackLocation("서울특별시 중구", 37.5665, 126.9780, "서울", "시청", "중구"),
            new FallbackLocation("강남역", 37.4979, 127.0276, "강남", "강남역"),
            new FallbackLocation("홍대입구역", 37.5572, 126.9245, "홍대", "홍대입구"),
            new FallbackLocation("인천광역시 연수구 송도", 37.3827, 126.6561, "인천", "송도"),
            new FallbackLocation("수원시 팔달구", 37.2636, 127.0286, "수원", "팔달"),
            new FallbackLocation("춘천시", 37.8813, 127.7300, "춘천"),
            new FallbackLocation("청주시 상당구", 36.6357, 127.4917, "청주"),
            new FallbackLocation("대전광역시 유성구", 36.3504, 127.3845, "대전", "유성"),
            new FallbackLocation("세종특별자치시", 36.4800, 127.2890, "세종"),
            new FallbackLocation("전주시 완산구 한옥마을", 35.8150, 127.1531, "전주", "한옥마을"),
            new FallbackLocation("광주광역시 동구 충장로", 35.1461, 126.9200, "광주", "충장"),
            new FallbackLocation("대구광역시 중구 동성로", 35.8691, 128.5976, "대구", "동성"),
            new FallbackLocation("부산광역시 해운대구", 35.1631, 129.1635, "부산", "해운대"),
            new FallbackLocation("울산광역시 남구", 35.5384, 129.3114, "울산", "남구"),
            new FallbackLocation("창원시 성산구", 35.2280, 128.6811, "창원", "성산"),
            new FallbackLocation("제주특별자치도 제주시", 33.4996, 126.5312, "제주", "제주시")
    );

    @Value("${location.kakao.rest-api-key:}")
    private String kakaoRestApiKey;

    private final RestTemplate restTemplate;

    @Cacheable(
            cacheNames = "locations",
            key = "#query == null ? '' : #query.trim().toLowerCase()",
            condition = "#query != null && !#query.isBlank()",
            sync = true
    )
    public List<LocationDto.Response> search(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("검색할 지역명을 입력해주세요.");
        }
        String normalizedQuery = query.trim();
        if (kakaoRestApiKey == null || kakaoRestApiKey.isBlank()) {
            return searchFallback(normalizedQuery);
        }

        try {
            List<LocationDto.Response> keywordResults = requestKakao(
                    "https://dapi.kakao.com/v2/local/search/keyword.json",
                    normalizedQuery
            );
            if (!keywordResults.isEmpty()) {
                return keywordResults;
            }

            List<LocationDto.Response> addressResults = requestKakao(
                    "https://dapi.kakao.com/v2/local/search/address.json",
                    normalizedQuery
            );
            return addressResults.isEmpty() ? searchFallback(normalizedQuery) : addressResults;
        } catch (Exception e) {
            log.warn("Kakao Local API search failed. Falling back to local suggestions. query={}", normalizedQuery, e);
            return searchFallback(normalizedQuery);
        }
    }

    public LocationDto.Response resolveCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < 33.0 || latitude > 39.5
                || longitude < 124.0 || longitude > 132.0) {
            throw new IllegalArgumentException("대한민국 범위의 위치 정보만 사용할 수 있습니다.");
        }

        Grid grid = convertToGrid(latitude, longitude);
        return LocationDto.Response.builder()
                .locationName("현재 위치")
                .latitude(latitude)
                .longitude(longitude)
                .nx(grid.nx())
                .ny(grid.ny())
                .build();
    }

    private List<LocationDto.Response> requestKakao(String url, String query) {
        URI uri = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("query", query)
                .queryParam("size", KAKAO_RESULT_SIZE)
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoRestApiKey);

        String response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        ).getBody();

        return parseKakaoResponse(response);
    }

    private List<LocationDto.Response> parseKakaoResponse(String response) {
        if (response == null || response.isBlank()) {
            return List.of();
        }
        JSONObject json = new JSONObject(response);
        JSONArray documents = json.optJSONArray("documents");
        if (documents == null) {
            return List.of();
        }
        List<LocationDto.Response> results = new ArrayList<>();

        for (int i = 0; i < documents.length(); i++) {
            JSONObject item = documents.optJSONObject(i);
            if (item == null) {
                continue;
            }
            try {
                double longitude = Double.parseDouble(item.optString("x"));
                double latitude = Double.parseDouble(item.optString("y"));
                Grid grid = convertToGrid(latitude, longitude);

                results.add(LocationDto.Response.builder()
                        .locationName(getLocationName(item))
                        .latitude(latitude)
                        .longitude(longitude)
                        .nx(grid.nx())
                        .ny(grid.ny())
                        .build());
            } catch (NumberFormatException ignored) {
                log.debug("Skipping Kakao location result without valid coordinates");
            }
        }

        return results;
    }

    private String getLocationName(JSONObject item) {
        String placeName = item.optString("place_name");
        if (!placeName.isBlank()) {
            return placeName;
        }
        String addressName = item.optString("address_name");
        if (!addressName.isBlank()) {
            return addressName;
        }
        return "선택 위치";
    }

    private List<LocationDto.Response> searchFallback(String query) {
        String normalized = query.replaceAll("\\s+", "").trim().toLowerCase(Locale.ROOT);
        return FALLBACK_LOCATIONS.stream()
                .filter(location -> location.matches(normalized))
                .limit(KAKAO_RESULT_SIZE)
                .map(location -> {
                    Grid grid = convertToGrid(location.latitude(), location.longitude());
                    return LocationDto.Response.builder()
                            .locationName(location.name())
                            .latitude(location.latitude())
                            .longitude(location.longitude())
                            .nx(grid.nx())
                            .ny(grid.ny())
                            .build();
                })
                .toList();
    }

    private Grid convertToGrid(double latitude, double longitude) {
        double earthRadius = 6371.00877;
        double grid = 5.0;
        double slat1 = 30.0;
        double slat2 = 60.0;
        double olon = 126.0;
        double olat = 38.0;
        double xo = 43;
        double yo = 136;
        double degToRad = Math.PI / 180.0;

        double re = earthRadius / grid;
        double slat1Rad = slat1 * degToRad;
        double slat2Rad = slat2 * degToRad;
        double olonRad = olon * degToRad;
        double olatRad = olat * degToRad;

        double sn = Math.tan(Math.PI * 0.25 + slat2Rad * 0.5) / Math.tan(Math.PI * 0.25 + slat1Rad * 0.5);
        sn = Math.log(Math.cos(slat1Rad) / Math.cos(slat2Rad)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1Rad * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1Rad) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olatRad * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + latitude * degToRad * 0.5);
        ra = re * sf / Math.pow(ra, sn);
        double theta = longitude * degToRad - olonRad;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + xo + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + yo + 0.5);
        return new Grid(nx, ny);
    }

    private record Grid(int nx, int ny) {
    }

    private record FallbackLocation(String name, double latitude, double longitude, String... aliases) {
        private boolean matches(String query) {
            String normalizedName = name.toLowerCase(Locale.ROOT);
            if (normalizedName.contains(query) || query.contains(normalizedName)) {
                return true;
            }
            for (String alias : aliases) {
                if (alias.toLowerCase(Locale.ROOT).contains(query) || query.contains(alias.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
            return false;
        }
    }
}

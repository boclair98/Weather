package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.LocationDto;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    @Value("${location.kakao.rest-api-key:}")
    private String kakaoRestApiKey;

    private final RestTemplate restTemplate;

    public List<LocationDto.Response> search(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("검색할 지역명을 입력해주세요.");
        }
        if (kakaoRestApiKey == null || kakaoRestApiKey.isBlank()) {
            throw new IllegalStateException("Kakao Local API 키가 설정되지 않았습니다.");
        }

        List<LocationDto.Response> keywordResults = requestKakao(
                "https://dapi.kakao.com/v2/local/search/keyword.json",
                query.trim()
        );
        if (!keywordResults.isEmpty()) {
            return keywordResults;
        }

        return requestKakao(
                "https://dapi.kakao.com/v2/local/search/address.json",
                query.trim()
        );
    }

    private List<LocationDto.Response> requestKakao(String url, String query) {
        URI uri = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("query", query)
                .queryParam("size", 5)
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
        JSONObject json = new JSONObject(response);
        JSONArray documents = json.getJSONArray("documents");
        List<LocationDto.Response> results = new ArrayList<>();

        for (int i = 0; i < documents.length(); i++) {
            JSONObject item = documents.getJSONObject(i);
            double longitude = Double.parseDouble(item.getString("x"));
            double latitude = Double.parseDouble(item.getString("y"));
            Grid grid = convertToGrid(latitude, longitude);

            results.add(LocationDto.Response.builder()
                    .locationName(getLocationName(item))
                    .latitude(latitude)
                    .longitude(longitude)
                    .nx(grid.nx())
                    .ny(grid.ny())
                    .build());
        }

        return results;
    }

    private String getLocationName(JSONObject item) {
        if (item.has("place_name") && !item.getString("place_name").isBlank()) {
            return item.getString("place_name");
        }
        if (item.has("address_name") && !item.getString("address_name").isBlank()) {
            return item.getString("address_name");
        }
        return "선택 위치";
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
}

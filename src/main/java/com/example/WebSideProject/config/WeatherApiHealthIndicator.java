package com.example.WebSideProject.config;

import com.example.WebSideProject.service.ExternalApiGuard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("weatherApi")
@Profile("prod")
public class WeatherApiHealthIndicator implements HealthIndicator {

    private final String weatherApiKey;
    private final String airQualityApiKey;
    private final String weatherWarningApiKey;
    private final String livingWeatherApiKey;
    private final String pollenApiKey;
    private final String kakaoRestApiKey;
    private final String kakaoMobilityApiKey;
    private final ExternalApiGuard externalApiGuard;

    public WeatherApiHealthIndicator(
            @Value("${weather.api.key:}") String weatherApiKey,
            @Value("${air-quality.api.key:}") String airQualityApiKey,
            @Value("${weather-safety.warning.api-key:}") String weatherWarningApiKey,
            @Value("${weather-safety.uv.api-key:}") String livingWeatherApiKey,
            @Value("${weather-safety.pollen.api-key:}") String pollenApiKey,
            @Value("${location.kakao.rest-api-key:}") String kakaoRestApiKey,
            @Value("${route.kakao-mobility.rest-api-key:}") String kakaoMobilityApiKey,
            ExternalApiGuard externalApiGuard
    ) {
        this.weatherApiKey = weatherApiKey;
        this.airQualityApiKey = airQualityApiKey;
        this.weatherWarningApiKey = weatherWarningApiKey;
        this.livingWeatherApiKey = livingWeatherApiKey;
        this.pollenApiKey = pollenApiKey;
        this.kakaoRestApiKey = kakaoRestApiKey;
        this.kakaoMobilityApiKey = kakaoMobilityApiKey;
        this.externalApiGuard = externalApiGuard;
    }

    @Override
    public Health health() {
        if (isBlank(weatherApiKey)) {
            return Health.down()
                    .withDetail("configuration", "WEATHER_API_KEY is required")
                    .build();
        }
        if (externalApiGuard.isCircuitOpen("kma-forecast")) {
            return Health.status("DEGRADED")
                    .withDetail("upstream", "KMA forecast circuit is open")
                    .withDetail("fallback", "last-known-good forecast may be served")
                    .build();
        }

        List<String> missingOptionalKeys = missingOptionalKeys();
        if (!missingOptionalKeys.isEmpty()) {
            return Health.status("DEGRADED")
                    .withDetail("configuration", "Optional integrations are not fully configured")
                    .withDetail("missingKeys", missingOptionalKeys)
                    .build();
        }

        return Health.up()
                .withDetail("upstream", "Weather integrations configured")
                .build();
    }

    private List<String> missingOptionalKeys() {
        List<String> missing = new ArrayList<>();
        addIfBlank(missing, airQualityApiKey, "AIR_QUALITY_API_KEY");
        addIfBlank(missing, weatherWarningApiKey, "WEATHER_WARNING_API_KEY");
        addIfBlank(missing, livingWeatherApiKey, "LIVING_WEATHER_API_KEY");
        addIfBlank(missing, pollenApiKey, "POLLEN_API_KEY");
        addIfBlank(missing, kakaoRestApiKey, "KAKAO_REST_API_KEY");
        addIfBlank(missing, kakaoMobilityApiKey, "KAKAO_MOBILITY_REST_API_KEY");
        return List.copyOf(missing);
    }

    private static void addIfBlank(List<String> missing, String value, String keyName) {
        if (isBlank(value)) {
            missing.add(keyName);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

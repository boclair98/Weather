package com.example.WebSideProject.config;

import com.example.WebSideProject.service.ExternalApiGuard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("weatherApi")
@Profile("prod")
public class WeatherApiHealthIndicator implements HealthIndicator {

    private final String apiKey;
    private final ExternalApiGuard externalApiGuard;

    public WeatherApiHealthIndicator(
            @Value("${weather.api.key:}") String apiKey,
            ExternalApiGuard externalApiGuard
    ) {
        this.apiKey = apiKey;
        this.externalApiGuard = externalApiGuard;
    }

    @Override
    public Health health() {
        if (apiKey == null || apiKey.isBlank()) {
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
        return Health.up().withDetail("upstream", "KMA forecast configured").build();
    }
}

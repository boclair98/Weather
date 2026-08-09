package com.example.WebSideProject.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("weatherApi")
@Profile("prod")
public class WeatherApiHealthIndicator implements HealthIndicator {

    private final String apiKey;

    public WeatherApiHealthIndicator(@Value("${weather.api.key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public Health health() {
        if (apiKey == null || apiKey.isBlank()) {
            return Health.down()
                    .withDetail("configuration", "WEATHER_API_KEY is required")
                    .build();
        }
        return Health.up().build();
    }
}

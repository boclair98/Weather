package com.example.WebSideProject.controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FrontendContractTest {

    @Test
    void exposesThreeDayPlannerShareAndPwaContracts() throws IOException {
        String html = classpathText("/templates/index.html");

        assertThat(html)
                .contains("/manifest.webmanifest")
                .contains("data-day-offset=\"0\"")
                .contains("data-day-offset=\"1\"")
                .contains("data-day-offset=\"2\"")
                .contains("id=\"weatherPlanner\"")
                .contains("id=\"currentWeather\"")
                .contains("/api/weather/current")
                .contains("/api/weather/planner")
                .contains("buildWeatherShareUrl")
                .contains("restoreSharedWeather")
                .contains("navigator.serviceWorker.register")
                .contains("href=\"#weatherSearch\"")
                .contains("prefers-reduced-motion")
                .contains("aria-busy");
        assertThat(html)
                .contains("th:attr=\"nonce=${cspNonce}\"")
                .contains("id=\"plannerQuality\"")
                .contains("id=\"privacyConsent\"")
                .contains("/api/users/me/data");
    }

    @Test
    void serviceWorkerKeepsApiResponsesNetworkOnly() throws IOException {
        String worker = classpathText("/static/service-worker.js");

        assertThat(worker)
                .contains("url.pathname.startsWith(\"/api/\")")
                .contains("fetch(request).catch")
                .contains("OFFLINE")
                .containsOnlyOnce("cache.put(request, copy)");
    }

    private String classpathText(String path) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Classpath resource not found: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

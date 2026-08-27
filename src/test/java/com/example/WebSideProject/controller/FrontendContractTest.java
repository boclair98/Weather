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
                .contains("/weather.css?v=20260827-verify-code-v1")
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
                .contains("id=\"openSubscribeNav\"")
                .contains("class=\"subscription-options\"")
                .contains("updateSubscriptionSubmitState")
                .contains("aria-pressed=\"true\"")
                .contains("aria-busy")
                .contains("id=\"personalizationSummary\"")
                .contains("id=\"decisionExplanation\"")
                .contains("id=\"riskFactorList\"")
                .contains("renderRiskFactors")
                .contains("id=\"intentHelper\"")
                .contains("data-activity-type=\"COMMUTE\"")
                .contains("const loginUrl")
                .contains("if (locations.length === 1)")
                .contains("아래에서 오늘의 판단을 확인하세요")
                .contains("정확한 위치를 선택해주세요")
                .contains("id=\"refreshWeather\"")
                .contains("id=\"unitToggle\"")
                .contains("id=\"themeToggle\"")
                .contains("id=\"previewBriefing\"")
                .contains("id=\"briefingPreviewDialog\"")
                .contains("id=\"favoriteLocation\"")
                .contains("id=\"snapshotNote\"")
                .contains("id=\"goOutWindow\"")
                .contains("renderGoOutWindow")
                .contains("useRecommendedWindow")
                .contains("id=\"emailVerificationCode\"")
                .contains("id=\"confirmEmailVerification\"")
                .contains("/api/users/email-verification/confirm")
                .contains("인증번호 6자리를 입력해주세요")
                .contains("startVerificationResendCooldown")
                .contains("초 후 다시 받기")
                .contains("VERIFICATION_COOLDOWN")
                .contains("TODAY'S CALL")
                .contains("requestJson")
                .contains("weather-last-snapshot-v2");
        assertThat(html)
                .contains("th:attr=\"nonce=${cspNonce}\"")
                .contains("id=\"plannerQuality\"")
                .contains("id=\"privacyConsent\"")
                .contains("/api/users/me/data");

        String css = classpathText("/static/weather.css");
        assertThat(css)
                .contains("--blue: #3182f6")
                .contains("#mainContent .dashboard-search-card")
                .contains("#mainContent .go-out-window")
                .contains("@media (max-width: 760px)")
                .contains(".intent-chip")
                .contains("@media (prefers-reduced-motion: reduce)");
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

package com.example.WebSideProject.controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FrontendContractTest {

    @Test
    void rendersTheCompletePageForSignedInAndAnonymousVisitors() {
        var resolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        var engine = new org.thymeleaf.spring6.SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        for (boolean signedIn : new boolean[]{false, true}) {
            var context = new org.thymeleaf.context.Context();
            context.setVariable("signedIn", signedIn);
            context.setVariable("loginUrl", "/test-login");
            context.setVariable("logoutUrl", "/test-logout");
            context.setVariable("cspNonce", "test-nonce");
            String rendered = engine.process("index", context);
            assertThat(rendered.trim()).endsWith("</html>");
            assertThat(rendered).contains("nonce=\"test-nonce\"", "function renderAirReadings", "weather-chart.js");
        }
    }

    @Test
    void exposesThreeDayPlannerShareAndPwaContracts() throws IOException {
        String html = classpathText("/templates/index.html");

        assertThat(html)
                .contains("/manifest.webmanifest")
                .contains("/weather.css?v=20261013-rollback-tablet-v1")
                .contains("data-day-offset=\"0\"")
                .contains("data-day-offset=\"1\"")
                .contains("data-day-offset=\"2\"")
                .contains("id=\"weatherPlanner\"")
                .contains("/api/weather/current")
                .contains("/api/weather/planner")
                .contains("buildWeatherShareUrl")
                .contains("restoreSharedWeather")
                .contains("navigator.serviceWorker.register")
                .contains("service-worker-v17.js")
                .contains("<dialog class=\"location-search-dialog\" id=\"weatherSearch\"")
                .contains("data-inline=\"true\"")
                .contains("className = \"place-search-action\"")
                .contains("다른 지역 검색")
                .contains("class=\"location-search-content\" id=\"locationSearchContent\"")
                .contains("function openLocationSearch()")
                .contains("locationSearchDialog.showModal()")
                .contains("weather-search-modal-open")
                .contains("locationSearchInvoker = document.activeElement")
                .contains("focusTarget?.focus({preventScroll: true})")
                .contains("id=\"heroWeatherLocation\" tabindex=\"-1\"")
                .contains("has-weather-location")
                .contains("id=\"closeLocationSearch\"")
                .contains("id=\"heroWeatherCard\"")
                .contains("id=\"heroWeatherTemperature\"")
                .contains("heroWeatherCard.dataset.weather")
                .contains("forecastDayOffset !== 0 || !currentObservation")
                .contains("id=\"heroWeatherObservedAt\"")
                .contains("id=\"weatherAirStation\"")
                .contains("class=\"secondary-insights\"")
                .contains("function weatherIconKind")
                .contains("container.dataset.kind = resolvedKind")
                .contains("function koreaDateTimeParts")
                .contains("id=\"clearLocationQuery\"")
                .contains("koreaDateTimeParts")
                .contains("timeZone: \"Asia/Seoul\"")
                .contains("id=\"mobileWeatherNav\"")
                .contains("id=\"openSubscribeMobile\"")
                .contains("id=\"toggleHourlyForecast\"")
                .contains("renderHourlyForecastItems")
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
                .contains("const loginUrl")
                .contains("if (locations.length === 1)")
                .contains("날씨를 불러옵니다.")
                .contains("날씨를 볼 장소를 골라주세요")
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
                .contains("id=\"subscribeGoOutWindow\"")
                .contains("/api/users/me/delivery")
                .contains("prepareRecommendedAlert")
                .contains("subscriptionSource")
                .contains("id=\"emailVerificationCode\"")
                .contains("id=\"confirmEmailVerification\"")
                .contains("/api/users/email-verification/confirm")
                .contains("인증번호 6자리를 입력해주세요")
                .contains("startVerificationResendCooldown")
                .contains("초 후 다시 받기")
                .contains("VERIFICATION_COOLDOWN")
                .contains("requestJson")
                .contains("weather-last-snapshot-v2");
        assertThat(html)
                .contains("th:attr=\"nonce=${cspNonce}\"")
                .contains("id=\"plannerQuality\"")
                .contains("id=\"privacyConsent\"")
                .contains("/api/users/me/data");

        String css = classpathText("/static/weather.css");
        assertThat(css)
                .contains("--blue: #2878a4")
                .contains(".visually-hidden")
                .contains("#mainContent .dashboard-search-card")
                .contains("#mainContent .go-out-window")
                .contains("#mainContent .mobile-weather-nav")
                .contains("#mainContent .weather-now-card")
                .contains("#mainContent .weather-now-metrics span + span")
                .contains(".weather-icon[data-kind=\"unknown\"]")
                .contains("body[data-weather-theme=\"rain\"]")
                .contains("#mainContent .hourly-track")
                .contains("#mainContent .air-quality-summary")
                .contains("content-visibility: auto")
                .contains("@media (min-width: 761px) and (max-width: 1023px)")
                .contains("#mainContent.has-weather-location .daily-periods")
                .contains("@media (max-width: 760px)")
                .contains("@media (prefers-reduced-motion: reduce)");

        String observatoryCss = classpathText("/static/weather-observatory.css");
        assertThat(observatoryCss)
                .contains("weather-search-modal-open .mobile-weather-nav")
                .contains("height: 100dvh")
                .contains(".location-search-content")
                .contains("overflow-y: auto")
                .contains(".location-results")
                .contains("max-height: none");
    }

    @Test
    void serviceWorkerKeepsApiResponsesNetworkOnly() throws IOException {
        String worker = classpathText("/static/service-worker-v17.js");

        assertThat(worker)
                .contains("weather-shell-v38")
                .contains("weather.css?v=20261013-rollback-tablet-v1")
                .contains("weather-observatory.css?v=20261122-first-use-v4")
                .contains("url.pathname.startsWith(\"/api/\")")
                .contains("request.mode === \"navigate\"")
                .contains("OFFLINE")
                .contains("caches.match(request)")
                .contains(".put(request, response.clone())");
        assertThat(worker).contains("/offline.html", "APP_SHELL.includes(url.pathname + url.search)")
                .doesNotContain("cache.put(\"/\"");
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

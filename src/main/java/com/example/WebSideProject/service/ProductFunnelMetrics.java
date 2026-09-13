package com.example.WebSideProject.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Product funnel counters deliberately contain no email, account, location,
 * or other personal data. Tags are restricted to a fixed allow-list so the
 * metrics backend cannot receive unbounded user-controlled values.
 */
@Component
@RequiredArgsConstructor
public class ProductFunnelMetrics {

    private final MeterRegistry meterRegistry;

    public void record(String step) {
        record(step, "direct");
    }

    public void record(String step, String source) {
        meterRegistry.counter(
                "weather.product.funnel",
                "step", normalizeStep(step),
                "source", normalizeSource(source)
        ).increment();
    }

    private String normalizeStep(String step) {
        return switch (step == null ? "" : step) {
            case "location_searched", "verification_requested", "verification_confirmed",
                    "subscription_completed", "subscription_cancelled" -> step;
            default -> "unknown";
        };
    }

    private String normalizeSource(String source) {
        return "weather_window".equals(source) ? "weather_window" : "direct";
    }
}

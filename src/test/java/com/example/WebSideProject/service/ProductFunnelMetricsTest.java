package com.example.WebSideProject.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductFunnelMetricsTest {

    @Test
    void recordsOnlyBoundedNonPersonalTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ProductFunnelMetrics metrics = new ProductFunnelMetrics(registry);

        metrics.record("subscription_completed", "weather_window");
        metrics.record("user-supplied-step", "user-supplied-source");

        assertThat(registry.get("weather.product.funnel")
                .tag("step", "subscription_completed")
                .tag("source", "weather_window")
                .counter().count()).isEqualTo(1);
        assertThat(registry.get("weather.product.funnel")
                .tag("step", "unknown")
                .tag("source", "direct")
                .counter().count()).isEqualTo(1);
    }
}

package com.example.WebSideProject.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalApiGuardTest {

    @Test
    void retriesOneTransientFailureAndRecovers() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ExternalApiGuard guard = new ExternalApiGuard(registry);
        AtomicInteger attempts = new AtomicInteger();

        String result = guard.execute("test-provider", () -> {
            if (attempts.incrementAndGet() == 1) throw new IllegalStateException("temporary");
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(attempts).hasValue(2);
        assertThat(guard.isCircuitOpen("test-provider")).isFalse();
    }

    @Test
    void opensCircuitAfterRepeatedProviderFailures() {
        ExternalApiGuard guard = new ExternalApiGuard(new SimpleMeterRegistry());

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> guard.execute("test-provider", () -> {
                throw new IllegalStateException("down");
            })).isInstanceOf(ExternalApiGuard.ExternalApiUnavailableException.class);
        }

        assertThat(guard.isCircuitOpen("test-provider")).isTrue();
        assertThatThrownBy(() -> guard.execute("test-provider", () -> "never"))
                .isInstanceOf(ExternalApiGuard.ExternalApiUnavailableException.class)
                .hasMessageContaining("회로");
    }
}

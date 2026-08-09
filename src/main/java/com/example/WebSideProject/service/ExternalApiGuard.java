package com.example.WebSideProject.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

@Component
public class ExternalApiGuard {
    private static final int FAILURE_THRESHOLD = 3;
    private static final long OPEN_NANOS = Duration.ofSeconds(30).toNanos();
    private static final long RETRY_BACKOFF_NANOS = Duration.ofMillis(120).toNanos();

    private final MeterRegistry meterRegistry;
    private final Executor providerExecutor;
    private final Duration attemptTimeout;
    private final ConcurrentHashMap<String, CircuitState> circuits = new ConcurrentHashMap<>();

    public ExternalApiGuard(
            MeterRegistry meterRegistry,
            @Qualifier("providerExecutor") Executor providerExecutor,
            @Value("${app.external-api.attempt-timeout:7s}") Duration attemptTimeout
    ) {
        this.meterRegistry = meterRegistry;
        this.providerExecutor = providerExecutor;
        this.attemptTimeout = attemptTimeout;
    }

    public <T> T execute(String provider, Supplier<T> operation) {
        CircuitState state = circuits.computeIfAbsent(provider, ignored -> new CircuitState());
        long now = System.nanoTime();
        if (state.openUntilNanos.get() > now) {
            meterRegistry.counter("external.api.calls", "provider", provider, "result", "circuit_open")
                    .increment();
            throw new ExternalApiUnavailableException(provider + " 회로가 일시적으로 열려 있습니다.");
        }

        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                T result = executeWithDeadline(provider, operation);
                state.failures.set(0);
                state.openUntilNanos.set(0);
                meterRegistry.counter("external.api.calls", "provider", provider, "result", "success")
                        .increment();
                return result;
            } catch (RuntimeException failure) {
                lastFailure = failure;
                meterRegistry.counter("external.api.calls", "provider", provider, "result", "failure")
                        .increment();
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                if (attempt == 1) {
                    LockSupport.parkNanos(RETRY_BACKOFF_NANOS);
                }
            }
        }

        int failures = state.failures.incrementAndGet();
        if (failures >= FAILURE_THRESHOLD) {
            state.openUntilNanos.set(System.nanoTime() + OPEN_NANOS);
        }
        throw new ExternalApiUnavailableException(provider + " 호출에 실패했습니다.", lastFailure);
    }

    private <T> T executeWithDeadline(String provider, Supplier<T> operation) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(operation, providerExecutor);
        try {
            return future.get(attemptTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ExternalApiUnavailableException(provider + " 호출 제한시간을 초과했습니다.", e);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new ExternalApiUnavailableException(provider + " 호출이 중단되었습니다.", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new ExternalApiUnavailableException(provider + " 호출에 실패했습니다.", cause);
        }
    }

    public boolean isCircuitOpen(String provider) {
        CircuitState state = circuits.get(provider);
        return state != null && state.openUntilNanos.get() > System.nanoTime();
    }

    static final class CircuitState {
        private final AtomicInteger failures = new AtomicInteger();
        private final AtomicLong openUntilNanos = new AtomicLong();
    }

    public static class ExternalApiUnavailableException extends RuntimeException {
        public ExternalApiUnavailableException(String message) {
            super(message);
        }

        public ExternalApiUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

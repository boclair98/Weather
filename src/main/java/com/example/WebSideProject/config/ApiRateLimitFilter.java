package com.example.WebSideProject.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.security.MessageDigest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.HexFormat;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();
    private final int readLimit;
    private final int writeLimit;
    private final boolean distributed;
    private final StringRedisTemplate redisTemplate;
    private final Executor rateLimitExecutor;
    private final Duration redisDeadline;
    private final Cache<String, WindowCounter> counters = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofMinutes(3))
            .build();

    @Autowired
    public ApiRateLimitFilter(
            @Value("${app.rate-limit.read-per-minute:180}") int readLimit,
            @Value("${app.rate-limit.write-per-minute:20}") int writeLimit,
            @Value("${app.rate-limit.distributed:false}") boolean distributed,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            @Qualifier("rateLimitExecutor") Executor rateLimitExecutor,
            @Value("${app.rate-limit.redis-deadline:300ms}") Duration redisDeadline
    ) {
        this.readLimit = Math.max(1, readLimit);
        this.writeLimit = Math.max(1, writeLimit);
        this.distributed = distributed;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.rateLimitExecutor = rateLimitExecutor;
        this.redisDeadline = redisDeadline;
    }

    ApiRateLimitFilter(int readLimit, int writeLimit) {
        this.readLimit = Math.max(1, readLimit);
        this.writeLimit = Math.max(1, writeLimit);
        this.distributed = false;
        this.redisTemplate = null;
        this.rateLimitExecutor = Runnable::run;
        this.redisDeadline = Duration.ofSeconds(1);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        boolean readRequest = "GET".equalsIgnoreCase(request.getMethod());
        int limit = readRequest ? readLimit : writeLimit;
        String key = hashClientKey(clientKey(request)) + ":" + (readRequest ? "read" : "write");
        long count = increment(key);

        if (count > limit) {
            writeRateLimitResponse(response);
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - count)));
        filterChain.doFilter(request, response);
    }

    private long increment(String key) {
        if (distributed && redisTemplate != null) {
            try {
                return incrementDistributed(key);
            } catch (RuntimeException ignored) {
                // Availability wins over the distributed limiter. The bounded local limiter remains active.
            }
        }
        WindowCounter counter = counters.get(key, ignored -> new WindowCounter(System.currentTimeMillis()));
        return counter == null ? 1 : counter.increment(System.currentTimeMillis());
    }

    private long incrementDistributed(String key) {
        CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
            String redisKey = "rate-limit:" + key + ":" + (System.currentTimeMillis() / WINDOW_MILLIS);
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(70));
            }
            return count == null ? 1 : count;
        }, rateLimitExecutor);
        try {
            return future.get(redisDeadline.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IllegalStateException("분산 요청 제한기 응답 시간이 초과되었습니다.", e);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("분산 요청 제한기 호출이 중단되었습니다.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("분산 요청 제한기를 사용할 수 없습니다.", e.getCause());
        }
    }

    private String hashClientKey(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (Exception e) {
            throw new IllegalStateException("요청 식별자 해시 생성에 실패했습니다.", e);
        }
    }

    private String clientKey(HttpServletRequest request) {
        String codersUser = request.getHeader("X-Coders-User");
        if (codersUser != null && !codersUser.isBlank()) {
            return "user:" + codersUser.trim();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return "ip:" + forwarded.split(",", 2)[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        String requestId = RequestIdFilter.currentOrNew();
        response.setHeader(RequestIdFilter.HEADER, requestId);
        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":"
                + "\"요청이 너무 많습니다. 1분 뒤 다시 시도해주세요.\",\"requestId\":\""
                + requestId + "\"}");
    }

    private static final class WindowCounter {
        private long windowStartedAt;
        private int count;

        private WindowCounter(long windowStartedAt) {
            this.windowStartedAt = windowStartedAt;
        }

        private synchronized int increment(long now) {
            if (now - windowStartedAt >= WINDOW_MILLIS) {
                windowStartedAt = now;
                count = 0;
            }
            return ++count;
        }
    }
}

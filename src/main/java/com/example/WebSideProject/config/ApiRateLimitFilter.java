package com.example.WebSideProject.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();
    private final int readLimit;
    private final int writeLimit;
    private final Cache<String, WindowCounter> counters = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofMinutes(3))
            .build();

    public ApiRateLimitFilter(
            @Value("${app.rate-limit.read-per-minute:180}") int readLimit,
            @Value("${app.rate-limit.write-per-minute:20}") int writeLimit
    ) {
        this.readLimit = Math.max(1, readLimit);
        this.writeLimit = Math.max(1, writeLimit);
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
        String key = clientKey(request) + ":" + (readRequest ? "read" : "write");
        WindowCounter counter = counters.get(key, ignored -> new WindowCounter(System.currentTimeMillis()));

        if (counter != null && counter.increment(System.currentTimeMillis()) > limit) {
            writeRateLimitResponse(response);
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        filterChain.doFilter(request, response);
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

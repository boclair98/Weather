package com.example.WebSideProject.config;

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
import java.security.MessageDigest;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
public class AdminActuatorFilter extends OncePerRequestFilter {

    private final String adminApiKey;
    private final boolean adminRequireKey;

    public AdminActuatorFilter(
            @Value("${admin.api-key:}") String adminApiKey,
            @Value("${admin.require-key:false}") boolean adminRequireKey
    ) {
        this.adminApiKey = adminApiKey;
        this.adminRequireKey = adminRequireKey;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/actuator/metrics");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (isAuthorized(request.getHeader("X-Admin-Key"))) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"code\":\"ACCESS_DENIED\",\"message\":\"관리자 인증이 필요합니다.\"}"
        );
    }

    private boolean isAuthorized(String providedKey) {
        if (adminApiKey == null || adminApiKey.isBlank()) {
            return !adminRequireKey;
        }
        return providedKey != null && MessageDigest.isEqual(
                adminApiKey.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}

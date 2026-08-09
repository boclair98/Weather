package com.example.WebSideProject.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SecurityHeadersFilter extends OncePerRequestFilter {
    public static final String CSP_NONCE_ATTRIBUTE = "cspNonce";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        byte[] nonceBytes = new byte[18];
        SECURE_RANDOM.nextBytes(nonceBytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);
        request.setAttribute(CSP_NONCE_ATTRIBUTE, nonce);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), payment=(), usb=()");
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        if ("/service-worker.js".equals(request.getRequestURI())) {
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        }
        response.setHeader(
                "Content-Security-Policy",
                "default-src 'self'; style-src 'self' 'nonce-" + nonce + "'; " +
                        "script-src 'self' 'nonce-" + nonce + "'; connect-src 'self'; " +
                        "img-src 'self' data:; font-src 'self'"
        );
        filterChain.doFilter(request, response);
    }
}

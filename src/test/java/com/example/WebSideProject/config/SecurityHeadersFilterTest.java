package com.example.WebSideProject.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityHeadersFilterTest {

    private final SecurityHeadersFilter filter = new SecurityHeadersFilter();

    @Test
    void addsBrowserIsolationAndHttpsHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getHeader("Content-Security-Policy"))
                .contains("default-src 'self'")
                .contains("'nonce-")
                .doesNotContain("'unsafe-inline'");
        assertThat(request.getAttribute(SecurityHeadersFilter.CSP_NONCE_ATTRIBUTE))
                .isInstanceOf(String.class)
                .asString()
                .hasSize(24);
        assertThat(response.getHeader("Strict-Transport-Security")).contains("max-age=31536000");
        assertThat(response.getHeader("Permissions-Policy")).contains("camera=()");
        assertThat(response.getHeader("Cross-Origin-Opener-Policy")).isEqualTo("same-origin");
    }

    @Test
    void preventsStaleServiceWorkerCaching() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/service-worker.js");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getHeader("Cache-Control"))
                .isEqualTo("no-cache, no-store, must-revalidate");
    }
}

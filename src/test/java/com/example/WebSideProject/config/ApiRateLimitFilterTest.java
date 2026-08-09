package com.example.WebSideProject.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ApiRateLimitFilterTest {

    @Test
    void blocksApiRequestsAboveTheConfiguredLimit() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter(2, 1);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request("GET"), new MockHttpServletResponse(), chain);
        filter.doFilter(request("GET"), new MockHttpServletResponse(), chain);
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(request("GET"), blocked, chain);

        verify(chain, times(2)).doFilter(any(), any());
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");
        assertThat(blocked.getContentAsString()).contains("RATE_LIMITED");
    }

    @Test
    void usesSeparateBudgetForSignedInUsers() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter(1, 1);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest userA = request("GET");
        userA.addHeader("X-Coders-User", "user-a");
        MockHttpServletRequest userB = request("GET");
        userB.addHeader("X-Coders-User", "user-b");

        filter.doFilter(userA, new MockHttpServletResponse(), chain);
        MockHttpServletResponse responseB = new MockHttpServletResponse();
        filter.doFilter(userB, responseB, chain);

        assertThat(responseB.getStatus()).isEqualTo(200);
        verify(chain, times(2)).doFilter(any(), any());
    }

    private MockHttpServletRequest request(String method) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/weather");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}

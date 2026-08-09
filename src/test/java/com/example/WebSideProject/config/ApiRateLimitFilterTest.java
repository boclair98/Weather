package com.example.WebSideProject.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void usesRedisAndHashesIdentityForDistributedLimit() throws Exception {
        @SuppressWarnings("unchecked")
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(operations);
        when(operations.increment(any())).thenReturn(1L);
        ApiRateLimitFilter filter = new ApiRateLimitFilter(5, 2, true, provider);
        MockHttpServletRequest request = request("GET");
        request.addHeader("X-Coders-User", "sensitive-user-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(operations).increment(key.capture());
        assertThat(key.getValue()).startsWith("rate-limit:").doesNotContain("sensitive-user-id");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("4");
    }

    private MockHttpServletRequest request(String method) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/weather");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}

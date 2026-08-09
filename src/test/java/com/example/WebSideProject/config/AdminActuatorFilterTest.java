package com.example.WebSideProject.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AdminActuatorFilterTest {

    @Test
    void rejectsMetricsWithoutAdminKeyInProductionMode() throws Exception {
        AdminActuatorFilter filter = new AdminActuatorFilter("secret", true);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/metrics");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("ACCESS_DENIED");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void acceptsMetricsWithMatchingAdminKey() throws Exception {
        AdminActuatorFilter filter = new AdminActuatorFilter("secret", true);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/metrics/weather.planner.generation");
        request.addHeader("X-Admin-Key", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doesNotProtectPublicHealthProbe() throws Exception {
        AdminActuatorFilter filter = new AdminActuatorFilter("secret", true);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void protectsPrometheusEndpointToo() throws Exception {
        AdminActuatorFilter filter = new AdminActuatorFilter("secret", true);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/prometheus");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("ACCESS_DENIED");
        verify(chain, never()).doFilter(request, response);
    }
}

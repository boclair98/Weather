package com.example.WebSideProject.controller;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class HomeControllerTest {

    @Test
    void visitorGetsWeatherViewAndCspNonce() {
        HomeController controller = new HomeController();
        ConcurrentModel model = new ConcurrentModel();

        MockHttpServletRequest request = requestWithNonce();
        String view = controller.home(request, model);

        assertThat(view).isEqualTo("index");
        assertThat(model.getAttribute("cspNonce")).isEqualTo("test-nonce");
    }

    private MockHttpServletRequest requestWithNonce() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("cspNonce", "test-nonce");
        return request;
    }
}

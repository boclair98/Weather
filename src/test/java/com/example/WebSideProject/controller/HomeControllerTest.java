package com.example.WebSideProject.controller;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class HomeControllerTest {

    @Test
    void anonymousVisitorGetsGoogleLoginUi() {
        HomeController controller = new HomeController("https://weather.coders.kr");
        ConcurrentModel model = new ConcurrentModel();

        MockHttpServletRequest request = requestWithNonce();
        String view = controller.home(null, request, model);

        assertThat(view).isEqualTo("index");
        assertThat(model.getAttribute("signedIn")).isEqualTo(false);
        assertThat(model.getAttribute("codersPlatform")).isEqualTo(true);
        assertThat(model.getAttribute("cspNonce")).isEqualTo("test-nonce");
        assertThat(model.getAttribute("loginUrl").toString())
                .startsWith("https://coders.kr/oauth/login/google")
                .contains("return_to=https://weather.coders.kr/");
    }

    @Test
    void validatedCodersIdentityIsShownAsSignedIn() {
        HomeController controller = new HomeController("https://weather.coders.kr/");
        ConcurrentModel model = new ConcurrentModel();

        controller.home("validated-user-id", requestWithNonce(), model);

        assertThat(model.getAttribute("signedIn")).isEqualTo(true);
        assertThat(model.getAttribute("logoutUrl").toString())
                .startsWith("https://mcp.coders.kr/sso/logout");
    }

    private MockHttpServletRequest requestWithNonce() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("cspNonce", "test-nonce");
        return request;
    }
}

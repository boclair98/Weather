package com.example.WebSideProject.controller;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class HomeControllerTest {

    @Test
    void usesPublicBaseUrlForCodersLoginReturnUrl() {
        HomeController controller = new HomeController();
        ReflectionTestUtils.setField(controller, "codersIdentityRequired", true);
        ReflectionTestUtils.setField(controller, "appBaseUrl", "https://boclair-weather.coders.kr");
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.home("coders-user", model);

        assertThat(view).isEqualTo("index");
        assertThat(model.getAttribute("signedIn")).isEqualTo(true);
        assertThat(model.getAttribute("logoutUrl").toString())
                .contains("return_to=https://boclair-weather.coders.kr/");
        assertThat(model.getAttribute("logoutUrl").toString())
                .doesNotContain("svc.cluster.local");
    }
}

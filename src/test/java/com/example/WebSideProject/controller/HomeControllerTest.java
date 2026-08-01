package com.example.WebSideProject.controller;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;

import static org.assertj.core.api.Assertions.assertThat;

class HomeControllerTest {

    @Test
    void homeDoesNotExposeCodersLoginUi() {
        HomeController controller = new HomeController();
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.home(model);

        assertThat(view).isEqualTo("index");
        assertThat(model.getAttribute("signedIn")).isEqualTo(false);
        assertThat(model.getAttribute("codersPlatform")).isEqualTo(false);
        assertThat(model.containsAttribute("loginUrl")).isFalse();
        assertThat(model.containsAttribute("logoutUrl")).isFalse();
    }
}

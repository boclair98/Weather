package com.example.WebSideProject.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class HomeController {

    private static final String GOOGLE_LOGIN_URL = "https://coders.kr/oauth/login/google";
    private static final String LOGOUT_URL = "https://mcp.coders.kr/sso/logout";

    private final String appBaseUrl;

    public HomeController(@Value("${app.base-url:http://localhost:8080}") String appBaseUrl) {
        this.appBaseUrl = appBaseUrl.replaceAll("/+$", "");
    }

    @GetMapping("/")
    public String home(
            @RequestHeader(value = "X-Coders-User", required = false) String codersUserId,
            Model model
    ) {
        boolean signedIn = codersUserId != null && !codersUserId.isBlank();
        String returnUrl = appBaseUrl + "/";
        model.addAttribute("codersPlatform", true);
        model.addAttribute("signedIn", signedIn);
        model.addAttribute("loginUrl", buildPlatformUrl(GOOGLE_LOGIN_URL, returnUrl));
        model.addAttribute("logoutUrl", buildPlatformUrl(LOGOUT_URL, returnUrl));
        return "index";
    }

    private String buildPlatformUrl(String baseUrl, String returnUrl) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("return_to", returnUrl)
                .build()
                .encode()
                .toUriString();
    }
}

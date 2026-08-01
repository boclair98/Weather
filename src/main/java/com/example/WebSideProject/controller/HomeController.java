package com.example.WebSideProject.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
public class HomeController {

    @Value("${coders.identity.required:false}")
    private boolean codersIdentityRequired;

    @GetMapping("/")
    public String home(
            @RequestHeader(value = "X-Coders-User", required = false) String codersUserId,
            Model model
    ) {
        boolean signedIn = codersUserId != null && !codersUserId.isBlank();
        String returnTo = ServletUriComponentsBuilder.fromCurrentRequest()
                .replaceQuery(null)
                .build()
                .toUriString();

        model.addAttribute("codersPlatform", codersIdentityRequired);
        model.addAttribute("signedIn", signedIn);
        model.addAttribute("loginUrl", authUrl("/sso/login", returnTo));
        model.addAttribute("logoutUrl", authUrl("/sso/logout", returnTo));
        return "index";
    }

    private String authUrl(String path, String returnTo) {
        return UriComponentsBuilder
                .fromHttpUrl("https://mcp.coders.kr" + path)
                .queryParam("return_to", returnTo)
                .build()
                .encode()
                .toUriString();
    }
}

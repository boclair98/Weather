package com.example.WebSideProject.controller;

import com.example.WebSideProject.config.SecurityHeadersFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(HttpServletRequest request, Model model) {
        model.addAttribute("cspNonce", request.getAttribute(SecurityHeadersFilter.CSP_NONCE_ATTRIBUTE));
        return "index";
    }
}

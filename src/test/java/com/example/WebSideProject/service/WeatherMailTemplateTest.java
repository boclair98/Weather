package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.WeatherDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherMailTemplateTest {

    private SpringTemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
    }

    @Test
    void rendersMobileFriendlySafetyBriefingWithoutFragileEmailCss() throws IOException {
        WeatherDto weather = WeatherDto.builder()
                .date("20260809")
                .periodLabel("아침")
                .sky("1")
                .pty("0")
                .tmp("27")
                .pop("10")
                .reh("70")
                .wsd("2.4")
                .uvIndex(10)
                .pollenType("잡초류")
                .pollenRiskLevel(0)
                .weatherWarningTitle("폭염경보")
                .weatherWarningDetails("서울 지역에 폭염경보가 발효 중입니다.")
                .build();

        Context context = new Context();
        context.setVariable("name", "테스트 사용자");
        context.setVariable("email", "test@example.com");
        context.setVariable("locationName", "강남역");
        context.setVariable("weather", weather);
        context.setVariable("temperatureSensitivityLabel", "보통");
        context.setVariable("activityTypeLabel", "편안한 일상");
        context.setVariable("smartAlertSummary", weather.getSmartAlertSummary());
        context.setVariable("dashboardUrl", "https://weather.coders.kr");
        context.setVariable("unsubscribeUrl", "https://weather.coders.kr/api/users/unsubscribe?token=test");

        String html = templateEngine.process("weather-mail", context);
        Path preview = Path.of("build", "email-preview", "weather-mail.html");
        Files.createDirectories(preview.getParent());
        Files.writeString(preview, html, StandardCharsets.UTF_8);

        assertThat(html)
                .contains("30초 브리핑")
                .contains("강남역")
                .contains("폭염경보")
                .contains("10 · 매우 높음")
                .contains("날씨한편에서 다시 보기")
                .doesNotContain("display:grid")
                .doesNotContain("display:flex")
                .doesNotContain("linear-gradient")
                .doesNotContain("<script");
    }
}

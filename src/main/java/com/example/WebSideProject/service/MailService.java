package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.WeatherDto;
import com.example.WebSideProject.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final WeatherMailHistoryService weatherMailHistoryService;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Async
    public void sendWeatherMail(User user, WeatherDto weather) {
        try {
            WeatherDto styledWeather = weather.withStylePreference(user.getAgeGroup(), user.getGender());
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject(styledWeather.getForecastLabel() + " 날씨 알림");

            Context context = new Context();
            context.setVariable("name", valueOrDash(user.getName()));
            context.setVariable("email", user.getEmail());
            context.setVariable("locationName", valueOrDash(user.getLocationName()));
            context.setVariable("weather", styledWeather);
            context.setVariable("ageGroup", user.getAgeGroup());
            context.setVariable("gender", user.getGender());
            context.setVariable("ageGroupLabel", user.getAgeGroup().getLabel());
            context.setVariable("genderLabel", user.getGender().getLabel());
            context.setVariable("styleRecommendation", styledWeather.getStyleRecommendation());
            context.setVariable("unsubscribeUrl", appBaseUrl + "/api/users/unsubscribe?token=" + encode(user.getUnsubscribeToken()));

            String html = templateEngine.process("weather-mail", context);
            helper.setText(html, true);

            mailSender.send(message);
            weatherMailHistoryService.recordSuccess(user, weather);
            log.info("날씨 메일 발송 완료: {}", user.getEmail());

        } catch (MessagingException e) {
            weatherMailHistoryService.recordFailure(user, weather, e);
            log.error("메일 발송 실패: {}", user.getEmail(), e);
        } catch (Exception e) {
            weatherMailHistoryService.recordFailure(user, weather, e);
            log.error("메일 발송 중 예상치 못한 오류 발생: {}", user.getEmail(), e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String valueOrDash(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return "-";
        }
        return value;
    }
}

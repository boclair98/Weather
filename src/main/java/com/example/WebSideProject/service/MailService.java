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

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Async
    public void sendWeatherMail(User user, WeatherDto weather) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject("🌤️ " + weather.getForecastLabel() + " 날씨 알림");

            Context context = new Context();
            context.setVariable("name", user.getName());
            context.setVariable("email", user.getEmail());
            context.setVariable("locationName", user.getLocationName());
            context.setVariable("weather", weather);
            context.setVariable("unsubscribeUrl", appBaseUrl + "/api/users/unsubscribe?email=" + encode(user.getEmail()));

            String html = templateEngine.process("weather-mail", context);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("날씨 메일 발송 완료: {}", user.getEmail());

        } catch (MessagingException e) {
            log.error("메일 발송 실패: {}", user.getEmail(), e);
        } catch (Exception e) {
            log.error("메일 발송 중 예상치 못한 오류 발생: {}", user.getEmail(), e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

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
        sendWeatherMailInternal(user, weather, null);
    }

    @Async
    public void sendSmartAlertMail(User user, WeatherDto weather, String alertSummary) {
        sendWeatherMailInternal(user, weather, "[날씨 주의] " + alertSummary);
    }

    @Async
    public void sendEmailVerificationMail(String email, String verificationUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("날씨한편 이메일 인증");

            Context context = new Context();
            context.setVariable("email", email);
            context.setVariable("verificationUrl", verificationUrl);
            String html = templateEngine.process("email-verification-mail", context);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("이메일 인증 메일 발송 완료");
        } catch (MessagingException e) {
            log.error("이메일 인증 메일 발송 실패", e);
        } catch (Exception e) {
            log.error("이메일 인증 메일 처리 중 예상치 못한 오류", e);
        }
    }

    private void sendWeatherMailInternal(User user, WeatherDto weather, String subject) {
        try {
            WeatherDto styledWeather = weather.withStylePreference(
                    user.getAgeGroup(),
                    user.getGender(),
                    user.getTemperatureSensitivity(),
                    user.getActivityType()
            );
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject(subject == null
                    ? styledWeather.getForecastLabel() + " 날씨 알림"
                    : subject + " · " + valueOrDash(user.getLocationName()));

            Context context = new Context();
            context.setVariable("name", valueOrDash(user.getName()));
            context.setVariable("email", user.getEmail());
            context.setVariable("locationName", valueOrDash(user.getLocationName()));
            context.setVariable("weather", styledWeather);
            context.setVariable("ageGroup", user.getAgeGroup());
            context.setVariable("gender", user.getGender());
            context.setVariable("ageGroupLabel", user.getAgeGroup().getLabel());
            context.setVariable("genderLabel", user.getGender().getLabel());
            context.setVariable("temperatureSensitivityLabel", user.getTemperatureSensitivity().getLabel());
            context.setVariable("activityTypeLabel", user.getActivityType().getLabel());
            context.setVariable("styleRecommendation", styledWeather.getStyleRecommendation());
            context.setVariable("smartAlertSummary", styledWeather.getSmartAlertSummary());
            context.setVariable("dashboardUrl", appBaseUrl);
            context.setVariable("unsubscribeUrl", appBaseUrl + "/api/users/unsubscribe?token=" + encode(user.getUnsubscribeToken()));

            String html = templateEngine.process("weather-mail", context);
            helper.setText(html, true);

            mailSender.send(message);
            weatherMailHistoryService.recordSuccess(user, weather);
            log.info("날씨 메일 발송 완료: userId={}", user.getId());

        } catch (MessagingException e) {
            weatherMailHistoryService.recordFailure(user, weather, e);
            log.error("메일 발송 실패: userId={}", user.getId(), e);
        } catch (Exception e) {
            weatherMailHistoryService.recordFailure(user, weather, e);
            log.error("메일 발송 중 예상치 못한 오류 발생: userId={}", user.getId(), e);
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

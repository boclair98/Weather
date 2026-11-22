package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.WeatherDto;
import com.example.WebSideProject.entity.User;
import jakarta.mail.Session;
import jakarta.mail.Part;
import jakarta.mail.Multipart;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MailServiceTest {
    @Test
    void assemblesUtf8PlainAndHtmlAlternativesWithoutASmtpConnection() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        WeatherService weatherService = mock(WeatherService.class);
        WeatherMailHistoryService histories = mock(WeatherMailHistoryService.class);
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        MailService service = new MailService(sender, engine, histories, weatherService);
        ReflectionTestUtils.setField(service, "appBaseUrl", "https://weather.coders.kr");
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(Instant.parse("2026-10-23T05:30:00Z"), ZoneOffset.UTC));
        User user = User.builder().email("preview@example.com").name("미리보기").locationName("강남역").nx(61).ny(125).build();
        ReflectionTestUtils.setField(user, "unsubscribeToken", "preview-only");
        WeatherDto weather = WeatherDto.builder().date("20261024").time("0900").sky("1").pty("0").tmp("15").pop("10").build();

        service.sendWeatherMail(user, weather);

        verify(weatherService).getHourlyWeather(61, 125, "강남역", 1);
        verify(sender).send(message);
        verify(histories).recordSuccess(user, weather);
        message.saveChanges();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        message.writeTo(bytes);
        // Round-trip the actual wire encoding, then inspect MIME metadata and
        // decoded text. Never include whole bodies or token URLs in failures.
        MimeMessage decoded = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(bytes.toByteArray()));
        Part alternatives = findPart(decoded, "multipart/alternative");
        assertThat(alternatives != null).as("Contains a multipart/alternative container").isTrue();
        assertThat(((Multipart) alternatives.getContent()).getCount()).isEqualTo(2);
        Part plainPart = findPart(decoded, "text/plain");
        Part htmlPart = findPart(decoded, "text/html");
        assertThat(plainPart != null && htmlPart != null).as("Contains plain and HTML alternatives").isTrue();
        assertThat(new ContentType(plainPart.getContentType()).getParameter("charset")).isEqualToIgnoringCase("UTF-8");
        assertThat(new ContentType(htmlPart.getContentType()).getParameter("charset")).isEqualToIgnoringCase("UTF-8");
        String plain = (String) plainPart.getContent();
        String html = (String) htmlPart.getContent();
        assertThat(plain.contains("날씨한편") && plain.contains("강남역") && plain.contains("내일의 핵심"))
                .as("Decoded plain alternative preserves Korean letter content").isTrue();
        assertThat(html.contains("날씨한편") && html.contains("강남역") && html.contains("내일의 날씨 편지"))
                .as("Decoded HTML alternative preserves Korean letter content").isTrue();
        assertThat(decoded.getSubject()).contains("날씨한편", "강남역", "10월 24일");
    }

    @Test
    void stillBuildsTheLetterWhenTheHourlyProviderFails() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        WeatherService weatherService = mock(WeatherService.class);
        when(weatherService.getHourlyWeather(anyInt(), anyInt(), anyString(), anyInt())).thenThrow(new IllegalStateException("offline"));
        WeatherMailHistoryService histories = mock(WeatherMailHistoryService.class);
        SpringTemplateEngine engine = mock(SpringTemplateEngine.class);
        when(engine.process(eq("weather-mail"), any(org.thymeleaf.context.IContext.class))).thenReturn("<p>확인된 예보</p>");
        MailService service = new MailService(sender, engine, histories, weatherService);
        ReflectionTestUtils.setField(service, "appBaseUrl", "https://weather.coders.kr");
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(Instant.parse("2026-10-23T05:30:00Z"), ZoneOffset.UTC));
        User user = User.builder().email("preview@example.com").name("미리보기").locationName("강남역").nx(61).ny(125).build();
        WeatherDto weather = WeatherDto.builder().date("20261023").time("1500").tmp("15").build();

        service.sendWeatherMail(user, weather);

        verify(sender).send(any(MimeMessage.class));
        verify(histories).recordSuccess(user, weather);
    }

    private Part findPart(Part part, String mimeType) throws Exception {
        if (part.isMimeType(mimeType)) return part;
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                Part found = findPart(multipart.getBodyPart(i), mimeType);
                if (found != null) return found;
            }
        }
        return null;
    }
}

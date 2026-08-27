package com.example.WebSideProject.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationMailTemplateTest {

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
    void rendersAReadableCodeWithoutAConfirmationLink() {
        Context context = new Context();
        context.setVariable("email", "test@example.com");
        context.setVariable("verificationCode", "042731");

        String html = templateEngine.process("email-verification-mail", context);

        assertThat(html)
                .contains("인증번호를 입력해 주세요")
                .contains("042731")
                .contains("15분")
                .doesNotContain("verificationUrl")
                .doesNotContain("이메일 인증하기")
                .doesNotContain("<a ");
    }
}

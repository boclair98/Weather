package com.example.WebSideProject.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserDtoPrivacyTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void subscriptionRequiresExplicitPrivacyConsent() {
        UserDto.RegisterRequest request = validRequest();
        request.setPrivacyConsent(false);

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("privacyConsent"));
    }

    @Test
    void acceptedConsentPassesPrivacyValidation() {
        UserDto.RegisterRequest request = validRequest();
        request.setPrivacyConsent(true);

        assertThat(validator.validate(request)).isEmpty();
    }

    private UserDto.RegisterRequest validRequest() {
        UserDto.RegisterRequest request = new UserDto.RegisterRequest();
        request.setName("사용자");
        request.setEmail("user@example.com");
        request.setNx(60);
        request.setNy(127);
        return request;
    }
}

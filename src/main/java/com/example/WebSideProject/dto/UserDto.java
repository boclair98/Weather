package com.example.WebSideProject.dto;

import com.example.WebSideProject.Enum.AgeGroup;
import com.example.WebSideProject.Enum.ActivityType;
import com.example.WebSideProject.Enum.GenderType;
import com.example.WebSideProject.Enum.TemperatureSensitivity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.time.LocalTime;

public class UserDto {

    @Getter
    @Setter
    public static class RegisterRequest {
        @NotBlank(message = "이름을 입력해주세요")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다")
        private String name;

        @Size(max = 2048, message = "이메일 입력값이 너무 깁니다")
        private String email;

        @Size(max = 128, message = "이메일 인증 토큰이 너무 깁니다")
        private String verificationToken;

        /**
         * 이메일로 받은 6자리 인증번호입니다. 이전 링크 인증 클라이언트와의
         * 호환성을 위해 verificationToken도 잠시 허용하지만, 새 화면은 이
         * 필드를 사용합니다.
         */
        @Pattern(regexp = "\\d{6}", message = "이메일 인증번호 6자리를 입력해주세요")
        private String verificationCode;

        @Size(max = 10, message = "한 번에 최대 10개의 이메일을 등록할 수 있습니다")
        private List<@Email(message = "올바른 이메일 형식이 아닙니다") @Size(max = 254, message = "이메일은 254자 이하여야 합니다") String> emails;

        @Min(value = 1, message = "위치를 선택해주세요")
        @Max(value = 200, message = "올바른 위치를 선택해주세요")
        private int nx = 60;

        @Min(value = 1, message = "위치를 선택해주세요")
        @Max(value = 300, message = "올바른 위치를 선택해주세요")
        private int ny = 127;

        @Size(max = 200, message = "위치명은 200자 이하여야 합니다")
        private String locationName = "서울특별시 중구";

        private Double latitude;

        private Double longitude;

        private AgeGroup ageGroup = AgeGroup.NONE;

        private GenderType gender = GenderType.NONE;

        private TemperatureSensitivity temperatureSensitivity = TemperatureSensitivity.NONE;

        private ActivityType activityType = ActivityType.DAILY;

        private boolean smartAlertEnabled = false;

        private boolean rainAlertEnabled = true;

        private boolean temperatureAlertEnabled = true;

        private boolean airQualityAlertEnabled = true;

        private boolean windAlertEnabled = true;

        private boolean morningEnabled = true;

        private boolean afternoonEnabled = false;

        private boolean eveningEnabled = false;

        @JsonFormat(pattern = "HH:mm")
        private LocalTime morningTime = LocalTime.of(6, 30);

        @JsonFormat(pattern = "HH:mm")
        private LocalTime afternoonTime = LocalTime.of(11, 30);

        @JsonFormat(pattern = "HH:mm")
        private LocalTime eveningTime = LocalTime.of(18, 30);

        @AssertTrue(message = "개인정보 수집·이용에 동의해주세요")
        private boolean privacyConsent;
    }

    @Getter
    @Setter
    public static class UpdateLocationRequest {
        @Email(message = "올바른 이메일 형식이 아닙니다")
        @NotBlank(message = "이메일을 입력해주세요")
        @Size(max = 254, message = "이메일은 254자 이하여야 합니다")
        private String email;

        @NotBlank(message = "위치명을 입력해주세요")
        @Size(max = 200, message = "위치명은 200자 이하여야 합니다")
        private String locationName;

        private Double latitude;

        private Double longitude;

        @Min(value = 1, message = "위치를 선택해주세요")
        @Max(value = 200, message = "올바른 위치를 선택해주세요")
        private int nx;

        @Min(value = 1, message = "위치를 선택해주세요")
        @Max(value = 300, message = "올바른 위치를 선택해주세요")
        private int ny;
    }

    @Getter
    @Setter
    public static class UpdateStylePreferenceRequest {
        @Email(message = "올바른 이메일 형식이 아닙니다")
        @NotBlank(message = "이메일을 입력해주세요")
        @Size(max = 254, message = "이메일은 254자 이하여야 합니다")
        private String email;

        private AgeGroup ageGroup = AgeGroup.NONE;

        private GenderType gender = GenderType.NONE;

        private TemperatureSensitivity temperatureSensitivity = TemperatureSensitivity.NONE;

        private ActivityType activityType = ActivityType.DAILY;
    }

    @Getter
    @Setter
    public static class UpdateNotificationRequest {
        private boolean morningEnabled;
        private boolean afternoonEnabled;
        private boolean eveningEnabled;

        @JsonFormat(pattern = "HH:mm")
        private LocalTime morningTime = LocalTime.of(6, 30);

        @JsonFormat(pattern = "HH:mm")
        private LocalTime afternoonTime = LocalTime.of(11, 30);

        @JsonFormat(pattern = "HH:mm")
        private LocalTime eveningTime = LocalTime.of(18, 30);
    }

    @Getter
    @Setter
    public static class UpdateSmartAlertRequest {
        private boolean smartAlertEnabled;
        private boolean rainAlertEnabled = true;
        private boolean temperatureAlertEnabled = true;
        private boolean airQualityAlertEnabled = true;
        private boolean windAlertEnabled = true;
    }

    @Getter
    @Setter
    public static class UnsubscribeRequest {
        @Size(max = 2048, message = "이메일 입력값이 너무 깁니다")
        private String email;

        @Size(max = 10, message = "한 번에 최대 10개의 이메일을 처리할 수 있습니다")
        private List<@Email(message = "올바른 이메일 형식이 아닙니다") @Size(max = 254, message = "이메일은 254자 이하여야 합니다") String> emails;
    }

    @Getter
    @Setter
    public static class EmailVerificationRequest {
        @NotBlank(message = "본인 이메일을 입력해주세요")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        @Size(max = 254, message = "이메일은 254자 이하여야 합니다")
        private String email;
    }

    @Getter
    @Setter
    public static class EmailVerificationConfirmRequest {
        @NotBlank(message = "본인 이메일을 입력해주세요")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        @Size(max = 254, message = "이메일은 254자 이하여야 합니다")
        private String email;

        @NotBlank(message = "이메일 인증번호를 입력해주세요")
        @Pattern(regexp = "\\d{6}", message = "이메일 인증번호 6자리를 입력해주세요")
        private String code;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String email;
        private boolean subscribed;
        private String locationName;
        private Double latitude;
        private Double longitude;
        private int nx;
        private int ny;
        private AgeGroup ageGroup;
        private GenderType gender;
        private TemperatureSensitivity temperatureSensitivity;
        private ActivityType activityType;
        private boolean smartAlertEnabled;
        private boolean rainAlertEnabled;
        private boolean temperatureAlertEnabled;
        private boolean airQualityAlertEnabled;
        private boolean windAlertEnabled;
        private boolean morningEnabled;
        private boolean afternoonEnabled;
        private boolean eveningEnabled;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime morningTime;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime afternoonTime;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime eveningTime;
        private String message;
        private String privacyConsentVersion;
        private java.time.LocalDateTime privacyConsentAt;
    }

    @Getter
    @Builder
    public static class BatchResponse {
        private int successCount;
        private List<String> recipients;
        private String message;
    }
}

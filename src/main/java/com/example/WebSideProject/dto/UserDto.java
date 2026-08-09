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
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class UserDto {

    @Getter
    @Setter
    public static class RegisterRequest {
        @NotBlank(message = "이름을 입력해주세요")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다")
        private String name;

        @Size(max = 2048, message = "이메일 입력값이 너무 깁니다")
        private String email;

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
        private String message;
    }

    @Getter
    @Builder
    public static class BatchResponse {
        private int successCount;
        private List<String> recipients;
        private String message;
    }
}

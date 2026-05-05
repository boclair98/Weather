package com.example.WebSideProject.dto;

import com.example.WebSideProject.Enum.AgeGroup;
import com.example.WebSideProject.Enum.GenderType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class UserDto {

    @Getter
    @Setter
    public static class RegisterRequest {
        @NotBlank(message = "이름을 입력해주세요")
        private String name;

        @Email(message = "올바른 이메일 형식이 아닙니다")
        @NotBlank(message = "이메일을 입력해주세요")
        private String email;

        @Min(value = 1, message = "위치를 선택해주세요")
        private int nx = 60;

        @Min(value = 1, message = "위치를 선택해주세요")
        private int ny = 127;

        private String locationName = "서울특별시 중구";

        private Double latitude;

        private Double longitude;

        private AgeGroup ageGroup = AgeGroup.NONE;

        private GenderType gender = GenderType.NONE;

        private boolean morningEnabled = true;

        private boolean afternoonEnabled = false;

        private boolean eveningEnabled = false;
    }

    @Getter
    @Setter
    public static class UpdateLocationRequest {
        @Email(message = "올바른 이메일 형식이 아닙니다")
        @NotBlank(message = "이메일을 입력해주세요")
        private String email;

        @NotBlank(message = "위치명을 입력해주세요")
        private String locationName;

        private Double latitude;

        private Double longitude;

        @Min(value = 1, message = "위치를 선택해주세요")
        private int nx;

        @Min(value = 1, message = "위치를 선택해주세요")
        private int ny;
    }

    @Getter
    @Setter
    public static class UpdateStylePreferenceRequest {
        @Email(message = "올바른 이메일 형식이 아닙니다")
        @NotBlank(message = "이메일을 입력해주세요")
        private String email;

        private AgeGroup ageGroup = AgeGroup.NONE;

        private GenderType gender = GenderType.NONE;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String email;
        private boolean subscribed;
        private String locationName;
        private AgeGroup ageGroup;
        private GenderType gender;
        private boolean morningEnabled;
        private boolean afternoonEnabled;
        private boolean eveningEnabled;
        private String message;
    }
}

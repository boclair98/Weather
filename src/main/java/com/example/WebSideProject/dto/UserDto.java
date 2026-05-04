package com.example.WebSideProject.dto;

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
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String email;
        private boolean subscribed;
        private String locationName;
        private String message;
    }
}

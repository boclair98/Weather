package com.example.WebSideProject.dto;

import lombok.Builder;
import lombok.Getter;

public class LocationDto {

    @Getter
    @Builder
    public static class Response {
        private String locationName;
        private double latitude;
        private double longitude;
        private int nx;
        private int ny;
    }
}

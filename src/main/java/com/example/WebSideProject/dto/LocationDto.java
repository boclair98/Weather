package com.example.WebSideProject.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

public class LocationDto {

    @Getter
    @Builder
    @Jacksonized
    public static class Response {
        private String locationName;
        private String regionName;
        private double latitude;
        private double longitude;
        private int nx;
        private int ny;
    }
}

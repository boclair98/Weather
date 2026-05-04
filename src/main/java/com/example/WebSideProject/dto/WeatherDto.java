package com.example.WebSideProject.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeatherDto {
    private String date;
    private String time;
    private String sky;
    private String pty;
    private String tmp;
    private String tmn;
    private String tmx;
    private String pop;
    private String reh;
    private String wsd;

    public String getForecastLabel() {
        if (date == null || date.length() != 8 || time == null || time.length() != 4) {
            return "아침 예보";
        }

        return date.substring(4, 6) + "월 " + date.substring(6, 8) + "일 아침 예보";
    }

    public String getSkyDescription() {
        return switch (sky) {
            case "1" -> "맑음 ☀️";
            case "3" -> "구름많음 ⛅";
            case "4" -> "흐림 ☁️";
            default  -> "알 수 없음";
        };
    }

    public String getPtyDescription() {
        return switch (pty) {
            case "0" -> "없음";
            case "1" -> "비 🌧️";
            case "2" -> "비/눈 🌨️";
            case "3" -> "눈 ❄️";
            case "4" -> "소나기 🌦️";
            default  -> "알 수 없음";
        };
    }
}

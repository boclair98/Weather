package com.example.WebSideProject.Enum;

import java.time.LocalTime;

public enum WeatherPeriod {
    MORNING("아침", "0900", LocalTime.of(6, 30)),
    AFTERNOON("점심", "1200", LocalTime.of(11, 30)),
    EVENING("저녁", "1800", LocalTime.of(18, 30));

    private final String label;
    private final String targetTime;
    private final LocalTime sendTime;

    WeatherPeriod(String label, String targetTime, LocalTime sendTime) {
        this.label = label;
        this.targetTime = targetTime;
        this.sendTime = sendTime;
    }

    public String getLabel() {
        return label;
    }

    public String getTargetTime() {
        return targetTime;
    }

    public LocalTime getSendTime() {
        return sendTime;
    }

    public int getTargetHour() {
        return Integer.parseInt(targetTime.substring(0, 2));
    }

    public static WeatherPeriod fromLabel(String label) {
        for (WeatherPeriod period : values()) {
            if (period.label.equals(label)) {
                return period;
            }
        }
        return MORNING;
    }
}

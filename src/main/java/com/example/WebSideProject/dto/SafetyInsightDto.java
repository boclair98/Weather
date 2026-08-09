package com.example.WebSideProject.dto;

public record SafetyInsightDto(
        int uvIndex,
        String pollenType,
        int pollenRiskLevel,
        String weatherWarningTitle,
        String weatherWarningDetails
) {
    public static SafetyInsightDto empty() {
        return new SafetyInsightDto(-1, "-", -1, "-", "-");
    }

    public boolean hasWeatherWarning() {
        return weatherWarningTitle != null
                && !weatherWarningTitle.isBlank()
                && !"-".equals(weatherWarningTitle);
    }
}

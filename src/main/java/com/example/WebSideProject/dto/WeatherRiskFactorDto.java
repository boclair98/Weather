package com.example.WebSideProject.dto;

/**
 * A user-facing explanation of one factor that changed the personalized outing score.
 */
public record WeatherRiskFactorDto(
        String code,
        String label,
        String severity,
        int scoreImpact,
        String evidence,
        String action
) {
}

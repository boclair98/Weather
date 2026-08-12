package com.example.WebSideProject.dto;

import java.util.List;

/**
 * A decision-oriented forecast that compares possible departure windows around a user's schedule.
 */
public record WeatherDecisionDto(
        String locationName,
        String targetDate,
        String requestedStart,
        String recommendedStart,
        String recommendedEnd,
        int durationMinutes,
        int shiftMinutes,
        int requestedScore,
        int recommendedScore,
        int improvementPoints,
        String headline,
        String rationale,
        List<String> packingChecklist,
        List<WindowCandidate> candidates,
        ForecastProvenanceDto provenance
) {
    public record WindowCandidate(
            String start,
            String end,
            int score,
            String scoreLabel,
            int precipitationProbability,
            String precipitation,
            int temperature,
            double windSpeed,
            boolean recommended
    ) {
    }
}

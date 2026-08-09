package com.example.WebSideProject.dto;

import java.time.Instant;

public record InstitutionalBriefingDto(
        String schemaVersion,
        String requestId,
        Instant generatedAt,
        String locationName,
        ForecastProvenanceDto provenance,
        WeatherPlannerDto briefing,
        String usageNotice
) {
    public static InstitutionalBriefingDto from(String requestId, WeatherPlannerDto planner) {
        return new InstitutionalBriefingDto(
                "weather-briefing/1.0",
                requestId,
                Instant.now(),
                planner.locationName(),
                planner.provenance(),
                planner,
                "의사결정 보조자료이며 방재 판단에는 기상청 공식 특보와 담당기관 지침을 우선하세요."
        );
    }
}

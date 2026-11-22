package com.example.WebSideProject.dto;

import java.util.List;

/** A weather-first view of one day's hourly forecast. */
public record HourlyWeatherDto(
        String locationName,
        String forecastDate,
        String headline,
        String precipitationSummary,
        Integer minimumTemperature,
        Integer maximumTemperature,
        Integer maximumPrecipitationProbability,
        List<Hour> hours,
        ForecastProvenanceDto provenance
) {
    public record Hour(
            String time,
            Integer temperature,
            Integer precipitationProbability,
            String precipitation,
            Integer humidity,
            Double windSpeed,
            String sky
    ) {
    }
}

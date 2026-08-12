package com.example.WebSideProject.dto;

import java.util.List;

/** A weather-first view of one day's hourly forecast. */
public record HourlyWeatherDto(
        String locationName,
        String forecastDate,
        String headline,
        String precipitationSummary,
        int minimumTemperature,
        int maximumTemperature,
        int maximumPrecipitationProbability,
        List<Hour> hours,
        ForecastProvenanceDto provenance
) {
    public record Hour(
            String time,
            int temperature,
            int precipitationProbability,
            String precipitation,
            int humidity,
            double windSpeed,
            String sky
    ) {
    }
}

package com.example.WebSideProject.dto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record WeatherPlannerDto(
        String locationName,
        String recommendedDayLabel,
        String recommendedPeriodLabel,
        String headline,
        List<DayPlan> days,
        List<String> packingChecklist
) {

    public static WeatherPlannerDto from(String locationName, List<DailyWeatherDto> forecasts) {
        if (forecasts == null || forecasts.isEmpty()) {
            throw new IllegalArgumentException("3일 플래너를 만들 예보가 없습니다.");
        }

        List<DayPlan> plans = forecasts.stream().map(DayPlan::from).toList();
        DayPlan recommended = plans.stream()
                .max(Comparator.comparingInt(DayPlan::averageScore))
                .orElse(plans.get(0));

        return new WeatherPlannerDto(
                locationName == null || locationName.isBlank() ? "선택 위치" : locationName,
                recommended.dayLabel(),
                recommended.bestPeriodLabel(),
                recommended.dayLabel() + " " + recommended.bestPeriodLabel()
                        + "이 3일 중 가장 움직이기 좋아요.",
                plans,
                buildPackingChecklist(forecasts)
        );
    }

    private static List<String> buildPackingChecklist(List<DailyWeatherDto> forecasts) {
        Set<String> items = new LinkedHashSet<>();
        for (DailyWeatherDto daily : forecasts) {
            for (WeatherDto weather : List.of(daily.morning(), daily.afternoon(), daily.evening())) {
                if (weather.isOfficialWarningRisk()) items.add("기상특보 확인");
                if (weather.isRainRisk()) items.add("접이식 우산");
                if (weather.isAirQualityRisk()) items.add("KF 마스크");
                if (weather.isUvRisk()) items.add("선크림·모자");
                if (weather.isTemperatureRisk()) items.add("물·체온 조절 용품");
                if (weather.isWindRisk()) items.add("바람에 안전한 소지품");
            }
        }
        if (items.isEmpty()) items.add("기본 외출 준비");
        return new ArrayList<>(items);
    }

    public record DayPlan(
            String dayLabel,
            String forecastDate,
            int averageScore,
            String scoreLabel,
            String riskLevel,
            String bestPeriod,
            String bestPeriodLabel,
            int bestPeriodScore,
            String temperatureRange,
            String rainSummary,
            String keyAction
    ) {
        private static DayPlan from(DailyWeatherDto daily) {
            List<WeatherDto> periods = List.of(daily.morning(), daily.afternoon(), daily.evening());
            int average = (int) Math.round(periods.stream()
                    .mapToInt(WeatherDto::getOutingScore)
                    .average()
                    .orElse(0));
            int min = periods.stream().mapToInt(DayPlan::temperature).min().orElse(0);
            int max = periods.stream().mapToInt(DayPlan::temperature).max().orElse(0);

            return new DayPlan(
                    daily.dayLabel(),
                    daily.forecastDate(),
                    average,
                    scoreLabel(average),
                    riskLevel(average),
                    daily.bestPeriod(),
                    daily.bestPeriodLabel(),
                    daily.bestOutingScore(),
                    min + "°C ~ " + max + "°C",
                    daily.rainSummary(),
                    keyAction(periods, daily)
            );
        }

        private static int temperature(WeatherDto weather) {
            try {
                return Integer.parseInt(weather.getTmp());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        private static String scoreLabel(int score) {
            if (score >= 85) return "아주 좋음";
            if (score >= 70) return "무난";
            if (score >= 50) return "준비 필요";
            return "일정 조정 권장";
        }

        private static String riskLevel(int score) {
            if (score >= 80) return "LOW";
            if (score >= 60) return "MEDIUM";
            return "HIGH";
        }

        private static String keyAction(List<WeatherDto> periods, DailyWeatherDto daily) {
            if (periods.stream().anyMatch(WeatherDto::isOfficialWarningRisk)) {
                return "출발 전 공식 기상특보를 확인하세요.";
            }
            if (periods.stream().anyMatch(WeatherDto::isRainRisk)) {
                return "우산을 챙기고 " + daily.bestPeriodLabel() + " 이동을 우선하세요.";
            }
            if (periods.stream().anyMatch(WeatherDto::isAirQualityRisk)) {
                return "마스크를 챙기고 야외 체류를 짧게 조절하세요.";
            }
            if (periods.stream().anyMatch(WeatherDto::isUvRisk)) {
                return "한낮 자외선 차단을 준비하세요.";
            }
            int maximumTemperature = periods.stream().mapToInt(DayPlan::temperature).max().orElse(20);
            int minimumTemperature = periods.stream().mapToInt(DayPlan::temperature).min().orElse(20);
            if (maximumTemperature >= 28) {
                return "한낮 더위를 피하고 마실 물을 챙기세요.";
            }
            if (minimumTemperature <= 5) {
                return "보온 겉옷을 챙기고 체감 추위에 대비하세요.";
            }
            if (periods.stream().anyMatch(WeatherDto::isTemperatureRisk)) {
                return "시간대별 기온 차에 맞춰 겹쳐 입으세요.";
            }
            return daily.bestPeriodLabel() + "을 중심으로 야외 일정을 잡아보세요.";
        }
    }
}

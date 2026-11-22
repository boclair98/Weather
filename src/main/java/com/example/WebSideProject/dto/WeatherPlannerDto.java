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
        List<String> packingChecklist,
        ForecastProvenanceDto provenance
) {

    public static WeatherPlannerDto from(String locationName, List<DailyWeatherDto> forecasts) {
        if (forecasts == null || forecasts.isEmpty()) {
            throw new IllegalArgumentException("3일 플래너를 만들 예보가 없습니다.");
        }

        List<DayPlan> plans = forecasts.stream().map(DayPlan::from).toList();
        DayPlan recommended = plans.stream()
                .filter(plan -> plan.bestPeriodScore() >= 0 && !"UNAVAILABLE".equals(plan.bestPeriod()))
                .max(Comparator.comparingInt(DayPlan::averageScore))
                .orElse(null);
        boolean complete = forecasts.stream().allMatch(daily -> List.of(
                daily.morning(), daily.afternoon(), daily.evening()
        ).stream().allMatch(WeatherDto::hasCoreWeatherData));
        String headline = recommended == null
                ? "예보 자료가 부족해 3일 일정을 비교할 수 없어요."
                : complete
                    ? recommended.dayLabel() + " " + recommended.bestPeriodLabel() + "이 3일 중 가장 움직이기 좋아요."
                    : "확인된 예보에서는 " + recommended.dayLabel() + " " + recommended.bestPeriodLabel() + "을 참고해 주세요.";

        return new WeatherPlannerDto(
                locationName == null || locationName.isBlank() ? "선택 위치" : locationName,
                recommended == null ? "자료 부족" : recommended.dayLabel(),
                recommended == null ? "자료 부족" : recommended.bestPeriodLabel(),
                headline,
                plans,
                buildPackingChecklist(forecasts),
                ForecastProvenanceDto.from(forecasts)
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
        if (items.isEmpty()) {
            boolean anyCompletePeriod = forecasts.stream()
                    .flatMap(daily -> List.of(daily.morning(), daily.afternoon(), daily.evening()).stream())
                    .anyMatch(WeatherDto::hasCoreWeatherData);
            items.add(anyCompletePeriod ? "기본 외출 준비" : "외출 전 최신 예보 확인");
        }
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
                    .filter(score -> score >= 0)
                    .average()
                    .orElse(-1));
            List<Integer> temperatures = periods.stream().map(DayPlan::temperature)
                    .filter(value -> value != null).toList();
            String temperatureRange = temperatures.isEmpty() ? "기온 자료 없음"
                    : temperatures.stream().mapToInt(Integer::intValue).min().orElseThrow() + "°C ~ "
                        + temperatures.stream().mapToInt(Integer::intValue).max().orElseThrow() + "°C";
            boolean hasRecommendation = average >= 0 && daily.bestOutingScore() >= 0
                    && !"UNAVAILABLE".equals(daily.bestPeriod());

            return new DayPlan(
                    daily.dayLabel(),
                    daily.forecastDate(),
                    average,
                    scoreLabel(average),
                    riskLevel(average),
                    hasRecommendation ? daily.bestPeriod() : "UNAVAILABLE",
                    hasRecommendation ? daily.bestPeriodLabel() : "자료 부족",
                    hasRecommendation ? daily.bestOutingScore() : -1,
                    temperatureRange,
                    daily.rainSummary(),
                    keyAction(periods, daily)
            );
        }

        private static Integer temperature(WeatherDto weather) {
            String value = weather.getTmp();
            if (value == null || value.isBlank()) return null;
            try {
                int parsed = Integer.parseInt(value);
                return parsed >= -100 && parsed <= 100 ? parsed : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        private static String scoreLabel(int score) {
            if (score < 0) return "자료 없음";
            if (score >= 85) return "아주 좋음";
            if (score >= 70) return "무난";
            if (score >= 50) return "준비 필요";
            return "일정 조정 권장";
        }

        private static String riskLevel(int score) {
            if (score < 0) return "UNKNOWN";
            if (score >= 80) return "LOW";
            if (score >= 60) return "MEDIUM";
            return "HIGH";
        }

        private static String keyAction(List<WeatherDto> periods, DailyWeatherDto daily) {
            if (periods.stream().anyMatch(WeatherDto::isOfficialWarningRisk)) {
                return "출발 전 공식 기상특보를 확인하세요.";
            }
            if (periods.stream().anyMatch(WeatherDto::isRainRisk)) {
                if (daily.bestOutingScore() < 0 || "UNAVAILABLE".equals(daily.bestPeriod())) {
                    return "우산을 챙기고 출발 전 비 예보를 다시 확인하세요.";
                }
                return "우산을 챙기고 " + daily.bestPeriodLabel() + " 이동을 우선하세요.";
            }
            if (periods.stream().anyMatch(WeatherDto::isAirQualityRisk)) {
                return "마스크를 챙기고 야외 체류를 짧게 조절하세요.";
            }
            if (periods.stream().anyMatch(WeatherDto::isUvRisk)) {
                return "한낮 자외선 차단을 준비하세요.";
            }
            List<Integer> temperatures = periods.stream().map(DayPlan::temperature)
                    .filter(value -> value != null).toList();
            double maximumTemperature = temperatures.stream().mapToInt(Integer::intValue).max().orElse(Integer.MIN_VALUE);
            double minimumTemperature = temperatures.stream().mapToInt(Integer::intValue).min().orElse(Integer.MAX_VALUE);
            if (maximumTemperature >= 28) {
                return "한낮 더위를 피하고 마실 물을 챙기세요.";
            }
            if (minimumTemperature <= 5) {
                return "보온 겉옷을 챙기고 체감 추위에 대비하세요.";
            }
            if (periods.stream().anyMatch(WeatherDto::isTemperatureRisk)) {
                return "시간대별 기온 차에 맞춰 겹쳐 입으세요.";
            }
            if (daily.bestOutingScore() < 0 || "UNAVAILABLE".equals(daily.bestPeriod())) {
                return "예보 자료가 부족해 이동 전 기온과 강수를 다시 확인해 주세요.";
            }
            return daily.bestPeriodLabel() + "을 중심으로 야외 일정을 잡아보세요.";
        }
    }
}

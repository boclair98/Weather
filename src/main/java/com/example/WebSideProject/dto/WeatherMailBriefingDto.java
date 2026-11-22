package com.example.WebSideProject.dto;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Pure, deterministic editing of source weather into an email-sized daily letter. */
public record WeatherMailBriefingDto(
        String dateLabel,
        String editionLabel,
        String summaryLabel,
        String forecastTimeLabel,
        String headline,
        String temperature,
        String condition,
        String temperatureSummary,
        String precipitationSummary,
        String airQualitySummary,
        List<Hour> hours,
        String hourlyNotice,
        List<Detail> details,
        String issuedAtLabel,
        String fetchedAtLabel,
        String hourlySourceLabel,
        boolean fallback
) {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN);
    private static final DateTimeFormatter SOURCE_TIME = DateTimeFormatter.ofPattern("M월 d일 HH:mm");
    private static final String MISSING = "자료 없음";

    public record Hour(String time, String condition, String temperature, String precipitation) { }
    public record Detail(String label, String text) { }
    private record TimedHour(LocalTime time, HourlyWeatherDto.Hour source) { }

    /** -1 means no supported, date-matching hourly request can be made. */
    public static int hourlyDayOffset(WeatherDto weather, Clock clock) {
        LocalDate date = parseDate(weather.getDate());
        if (date == null) return -1;
        long days = ChronoUnit.DAYS.between(LocalDate.now(clock.withZone(KOREA_ZONE)), date);
        return days >= 0 && days <= 2 ? (int) days : -1;
    }

    public static WeatherMailBriefingDto from(WeatherDto weather, HourlyWeatherDto hourly, Clock clock) {
        Objects.requireNonNull(weather, "weather");
        Objects.requireNonNull(clock, "clock");
        LocalDateTime now = LocalDateTime.now(clock.withZone(KOREA_ZONE));
        LocalDate targetDate = parseDate(weather.getDate());
        LocalTime targetTime = parseTime(weather.getTime());
        List<TimedHour> upcoming = upcoming(hourly, targetDate, now);
        List<Hour> hours = upcoming.stream().limit(6).map(point -> new Hour(
                point.time().format(DateTimeFormatter.ofPattern("HH:mm")),
                hourCondition(point.source()),
                number(point.source().temperature(), "°C"),
                point.source().precipitationProbability() == null
                        ? MISSING : "강수 " + point.source().precipitationProbability() + "%"
        )).toList();

        List<Detail> details = new ArrayList<>();
        if (weather.isOfficialWarningRisk()) {
            details.add(new Detail("기상특보", weather.getWeatherWarningTitle()
                    + (present(weather.getWeatherWarningDetails()) ? " · " + weather.getWeatherWarningDetails() : "")));
        }
        if (numeric(weather.getWsd()) || numeric(weather.getReh())) {
            details.add(new Detail("바람과 습도", "바람 " + metric(weather.getWsd(), "m/s")
                    + " · 습도 " + metric(weather.getReh(), "%")));
        }
        if (numeric(weather.getPm10Value()) || numeric(weather.getPm25Value())) {
            details.add(new Detail("공기질 수치", "미세먼지 " + metric(weather.getPm10Value(), "㎍/㎥")
                    + " · 초미세먼지 " + metric(weather.getPm25Value(), "㎍/㎥")
                    + (present(weather.getAirQualityStation()) ? " · " + weather.getAirQualityStation() + " 측정소" : "")));
        }
        if (weather.getUvIndex() >= 0) {
            details.add(new Detail("자외선", weather.getUvDisplay()));
        }

        String hourlyNotice = hours.isEmpty()
                ? targetDate != null && targetDate.isBefore(now.toLocalDate())
                    ? "지난 날짜의 예보예요. 최신 시간별 예보는 사이트에서 확인해 주세요."
                    : "앞으로의 시간별 예보를 불러오지 못했어요. 확인된 예보는 위에 남겨두었어요."
                : "시간별 예보는 해당 시각의 예상값이에요. 모든 시각은 한국 시간(KST)이에요.";
        String range = numeric(weather.getTmn()) || numeric(weather.getTmx())
                ? "최저 " + metric(weather.getTmn(), "°") + " · 최고 " + metric(weather.getTmx(), "°")
                : "예보 기온 " + metric(weather.getTmp(), "°C");
        boolean fallback = weather.isFallbackData()
                || hourly != null && hourly.provenance() != null && hourly.provenance().fallback();
        String day = relativeDay(targetDate, now.toLocalDate());
        return new WeatherMailBriefingDto(
                targetDate == null ? "예보 날짜 확인 필요" : targetDate.format(DATE_LABEL),
                day == null ? "선택한 날의 날씨 편지" : day + "의 날씨 편지",
                day == null ? "예보 핵심" : day + "의 핵심",
                targetTime == null ? "예보 시각 확인 필요" : targetTime.format(DateTimeFormatter.ofPattern("HH:mm")) + " 예보",
                headline(weather, upcoming, targetDate, targetTime, now), metric(weather.getTmp(), "°C"), condition(weather.getPty(), weather.getSky()),
                range, precipitationSummary(weather, upcoming), airQuality(weather), hours, hourlyNotice,
                List.copyOf(details), sourceTime(weather.getForecastIssuedAt()), sourceTime(weather.getDataFetchedAt()),
                !hours.isEmpty() && hourly.provenance() != null
                        ? "시간별 예보 발표 " + sourceTime(hourly.provenance().forecastIssuedAt())
                            + " · 조회 " + sourceTime(hourly.provenance().fetchedAt())
                        : "",
                fallback
        );
    }

    public String plainText(String locationName, String dashboardUrl, String unsubscribeUrl) {
        StringBuilder text = new StringBuilder("날씨한편\n").append(dateLabel).append(" · ").append(forecastTimeLabel)
                .append("\n").append(locationName).append("\n\n").append(headline)
                .append("\n").append(temperature).append(" · ").append(condition)
                .append("\n\n").append(summaryLabel).append("\n기온: ").append(temperatureSummary)
                .append("\n강수: ").append(precipitationSummary)
                .append("\n공기질: ").append(airQualitySummary).append("\n\n앞으로의 날씨\n");
        hours.forEach(hour -> text.append(hour.time()).append(" · ").append(hour.condition()).append(" · ")
                .append(hour.temperature()).append(" · ").append(hour.precipitation()).append("\n"));
        text.append(hourlyNotice).append("\n");
        details.forEach(detail -> text.append("\n").append(detail.label()).append(": ").append(detail.text()));
        if (fallback) text.append("\n원천자료 연결이 지연되어 마지막으로 확인한 예보를 전해드려요.");
        text.append("\n\n예보 발표: ").append(issuedAtLabel).append("\n자료 조회: ").append(fetchedAtLabel)
                .append(hourlySourceLabel.isEmpty() ? "" : "\n" + hourlySourceLabel)
                .append("\n모든 시각은 한국 시간(KST) 기준입니다.")
                .append("\n\n날씨 자세히 보기: ").append(dashboardUrl)
                .append("\n날씨 알림 그만 받기: ").append(unsubscribeUrl);
        return text.toString();
    }

    private static List<TimedHour> upcoming(HourlyWeatherDto hourly, LocalDate targetDate, LocalDateTime now) {
        if (hourly == null || targetDate == null || hourly.hours() == null
                || !targetDate.equals(parseDate(hourly.forecastDate())) || targetDate.isBefore(now.toLocalDate())) return List.of();
        return hourly.hours().stream().filter(Objects::nonNull)
                .map(hour -> new TimedHour(parseTime(hour.time()), hour))
                .filter(point -> point.time() != null)
                .filter(point -> targetDate.isAfter(now.toLocalDate())
                        || !point.time().isBefore(now.toLocalTime().truncatedTo(ChronoUnit.MINUTES)))
                .sorted(Comparator.comparing(TimedHour::time)).toList();
    }

    private static String headline(WeatherDto weather, List<TimedHour> upcoming,
                                   LocalDate date, LocalTime time, LocalDateTime now) {
        if (weather.isOfficialWarningRisk()) return "기상특보를 먼저 확인해 주세요.";
        TimedHour rain = upcoming.stream().filter(point -> rainRisk(point.source())).findFirst().orElse(null);
        if (rain != null) return rain.time().getHour() + "시 무렵, 비나 눈에 대비해 주세요.";
        boolean selectedForecastUpcoming = date != null && time != null
                && !LocalDateTime.of(date, time).isBefore(now.truncatedTo(ChronoUnit.MINUTES));
        if (selectedForecastUpcoming && (numeric(weather.getPop()) && Double.parseDouble(weather.getPop()) >= 60
                || precipitationKnown(weather.getPty()) && !"0".equals(weather.getPty()))) {
            return time.getHour() + "시 예보에는 비나 눈 소식이 있어요.";
        }
        List<Integer> temperatures = upcoming.stream().limit(6).map(point -> point.source().temperature())
                .filter(Objects::nonNull).toList();
        if (!temperatures.isEmpty()) {
            int min = temperatures.stream().mapToInt(Integer::intValue).min().orElseThrow();
            int max = temperatures.stream().mapToInt(Integer::intValue).max().orElseThrow();
            return "앞으로의 기온은 " + min + (min == max ? "" : "~" + max) + "°C로 예상돼요.";
        }
        if (date != null && time != null && !selectedForecastUpcoming) return "지난 시각의 예보예요. 최신 날씨를 확인해 주세요.";
        if (!numeric(weather.getTmp()) || !numeric(weather.getPop())) return "일부 날씨 자료를 확인하고 있어요.";
        return "선택한 시각의 예보를 모아 전해드려요.";
    }

    private static String relativeDay(LocalDate date, LocalDate today) {
        if (date == null) return null;
        return switch ((int) ChronoUnit.DAYS.between(today, date)) {
            case 0 -> "오늘";
            case 1 -> "내일";
            case 2 -> "모레";
            default -> null;
        };
    }

    private static String precipitationSummary(WeatherDto weather, List<TimedHour> upcoming) {
        Integer max = upcoming.stream().map(point -> point.source().precipitationProbability())
                .filter(Objects::nonNull).max(Integer::compareTo).orElse(null);
        if (max != null) return "확인된 남은 예보 중 최고 " + max + "%";
        return numeric(weather.getPop()) ? metric(weather.getPop(), "%") + " · 선택 시각 기준" : MISSING;
    }

    private static String airQuality(WeatherDto weather) {
        String pm10 = grade(weather.getPm10Grade());
        String pm25 = grade(weather.getPm25Grade());
        return "미세 " + pm10 + " · 초미세 " + pm25;
    }

    private static String grade(String value) {
        if (value == null) return MISSING;
        return switch (value) { case "1" -> "좋음"; case "2" -> "보통"; case "3" -> "나쁨"; case "4" -> "매우 나쁨"; default -> MISSING; };
    }

    private static boolean rainRisk(HourlyWeatherDto.Hour hour) {
        return hour.precipitationProbability() != null && hour.precipitationProbability() >= 60
                || present(hour.precipitation()) && !List.of("없음", "강수 없음", MISSING, "알 수 없음").contains(hour.precipitation());
    }

    private static String hourCondition(HourlyWeatherDto.Hour hour) {
        if (present(hour.precipitation()) && !List.of("없음", "강수 없음", MISSING, "알 수 없음").contains(hour.precipitation())) return hour.precipitation();
        return present(hour.sky()) && !"알 수 없음".equals(hour.sky()) ? hour.sky() : MISSING;
    }

    private static String condition(String precipitation, String sky) {
        if (precipitation != null) {
            String value = switch (precipitation) { case "1" -> "비"; case "2" -> "비·눈"; case "3" -> "눈"; case "4" -> "소나기"; default -> null; };
            if (value != null) return value;
        }
        if (sky == null) return MISSING;
        return switch (sky) { case "1" -> "맑음"; case "3" -> "구름 많음"; case "4" -> "흐림"; default -> MISSING; };
    }

    private static boolean precipitationKnown(String value) { return value != null && List.of("0", "1", "2", "3", "4").contains(value); }
    private static boolean present(String value) { return value != null && !value.isBlank() && !"-".equals(value) && !"null".equalsIgnoreCase(value); }
    private static boolean numeric(String value) { return present(value) && value.matches("-?\\d+(?:\\.\\d+)?"); }
    private static String metric(String value, String unit) { return numeric(value) ? value.replaceFirst("\\.0$", "") + unit : MISSING; }
    private static String number(Number value, String unit) { return value == null ? MISSING : value + unit; }

    private static LocalDate parseDate(String value) {
        try { return LocalDate.parse(value, value != null && value.length() == 8 ? DateTimeFormatter.BASIC_ISO_DATE : DateTimeFormatter.ISO_LOCAL_DATE); }
        catch (Exception ignored) { return null; }
    }

    private static LocalTime parseTime(String value) {
        try { return LocalTime.parse(value, value != null && value.length() == 4 ? DateTimeFormatter.ofPattern("HHmm") : DateTimeFormatter.ISO_LOCAL_TIME); }
        catch (Exception ignored) { return null; }
    }

    private static String sourceTime(String value) {
        try { return Instant.parse(value).atZone(KOREA_ZONE).format(SOURCE_TIME) + " KST"; }
        catch (Exception ignored) { }
        try { return OffsetDateTime.parse(value).atZoneSameInstant(KOREA_ZONE).format(SOURCE_TIME) + " KST"; }
        catch (Exception ignored) { return MISSING; }
    }
}

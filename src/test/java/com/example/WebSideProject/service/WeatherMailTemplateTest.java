package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.HourlyWeatherDto;
import com.example.WebSideProject.dto.WeatherDto;
import com.example.WebSideProject.dto.WeatherMailBriefingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherMailTemplateTest {
    private SpringTemplateEngine templateEngine;
    private static final Clock AFTERNOON = Clock.fixed(Instant.parse("2026-10-23T05:30:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
    }

    @Test
    void rendersAnEditorialLetterAndWritesDesktopAndMobileInspectionFixtures() throws IOException {
        WeatherDto weather = baseWeather().build();
        String html = render(weather, hourly("2026-10-23", false), AFTERNOON,
                "서울특별시 강남구 역삼동 강남역 12번 출구 인근 · 테헤란로 보행자 광장", "long-location");
        assertThat(html).contains("오늘의 핵심", "앞으로의 날씨", "최저 12° · 최고 21°", "날씨 알림 그만 받기")
                .contains("15:00", "10월 23일 11:00 KST", "기상청 단기예보")
                .doesNotContain(">14:00<", "09:00", "외출 86점", "추천 색상", "display:grid", "display:flex", "linear-gradient", "<script", "null°C", "th:text");
        assertThat(html.indexOf("오늘의 핵심")).isLessThan(html.indexOf("앞으로의 날씨"));
    }

    @Test
    void rendersRainNightAndMissingDataWithoutInventingTomorrowOrGoodWeather() throws IOException {
        WeatherDto rain = baseWeather().pty("1").sky("4").pop("80")
                .weatherWarningTitle("호우주의보").weatherWarningDetails("예보 변경과 공식 안전수칙을 확인해 주세요.").build();
        String rainyHtml = render(rain, hourly("2026-10-23", true), AFTERNOON, "강남역", "rain");
        assertThat(rainyHtml).contains("호우주의보", "비", "기상특보를 먼저 확인");

        Clock night = Clock.fixed(Instant.parse("2026-10-23T13:30:00Z"), ZoneOffset.UTC);
        WeatherDto late = baseWeather().time("2300").periodLabel("저녁").tmp("12").build();
        String nightHtml = render(late, hourly("2026-10-23", false), night, "을지로3가", "night");
        assertThat(nightHtml).contains("23:00").doesNotContain(">14:00<", ">15:00<", "내일은", "아침 예보");

        WeatherDto missing = WeatherDto.builder().date("20261023").time("1800").fallbackData(true).build();
        HourlyWeatherDto incomplete = new HourlyWeatherDto("선택 지역", "2026-10-23", "", "", null, null, null,
                List.of(new HourlyWeatherDto.Hour("18:00", null, null, "자료 없음", null, null, "자료 없음")), null);
        String missingHtml = render(missing, incomplete, AFTERNOON, "자료가 일부 없는 지역", "missing");
        assertThat(missingHtml).contains("자료 없음", "temperature-missing", "마지막으로 확인한 예보")
                .doesNotContain("20°C", "강수 0%", ">맑음<", "null", "내일은");
    }

    @Test
    void escapesLocationAndDoesNotEraseValidForecastWhenHourlyFails() throws IOException {
        String html = render(baseWeather().build(), null, AFTERNOON, "<script>alert('x')</script>", "partial-failure");
        assertThat(html).contains("21°C", "앞으로의 시간별 예보를 불러오지 못했어요", "&lt;script&gt;")
                .doesNotContain("<script>");
    }

    @Test
    void tomorrowLetterUsesItsOwnDateLabelsIncludingWarningHeadlines() throws IOException {
        WeatherDto tomorrow = baseWeather().date("20261024").time("0900")
                .weatherWarningTitle("호우주의보").weatherWarningDetails("기상특보를 확인해 주세요.").build();
        String html = render(tomorrow, hourly("2026-10-24", true), AFTERNOON, "강남역", "tomorrow");
        assertThat(html).contains("내일의 날씨 편지", "내일의 핵심", "10월 24일", "기상특보를 먼저 확인")
                .doesNotContain("오늘의 날씨 편지", "오늘의 핵심", "오늘은 기상특보");
    }

    private String render(WeatherDto weather, HourlyWeatherDto hourly, Clock clock, String location, String file) throws IOException {
        Context context = new Context();
        context.setVariable("locationName", location);
        context.setVariable("email", "preview@example.com");
        context.setVariable("weather", weather);
        context.setVariable("briefing", WeatherMailBriefingDto.from(weather, hourly, clock));
        context.setVariable("dashboardUrl", "https://weather.coders.kr");
        context.setVariable("unsubscribeUrl", "https://weather.coders.kr/api/users/unsubscribe?token=preview-only");
        String html = templateEngine.process("weather-mail", context);
        Path preview = Path.of("build", "reports", "weather-letter", file + ".html");
        Files.createDirectories(preview.getParent());
        Files.writeString(preview, html, StandardCharsets.UTF_8);
        return html;
    }

    private WeatherDto.WeatherDtoBuilder baseWeather() {
        return WeatherDto.builder().date("20261023").time("1500").periodLabel("점심")
                .sky("3").pty("0").tmp("21").tmn("12").tmx("21").pop("10").reh("55").wsd("2.4")
                .pm10Value("32").pm10Grade("2").pm25Value("11").pm25Grade("1").airQualityStation("역삼동")
                .uvIndex(4).forecastIssuedAt("2026-10-23T11:00:00+09:00").dataFetchedAt("2026-10-23T05:20:00Z");
    }

    private HourlyWeatherDto hourly(String date, boolean rain) {
        return new HourlyWeatherDto("강남역", date, "", "", 12, 21, rain ? 80 : 10, List.of(
                new HourlyWeatherDto.Hour("09:00", 15, 10, "없음", 55, 2.4, "구름 많음"),
                new HourlyWeatherDto.Hour("14:00", 21, rain ? 60 : 10, rain ? "비" : "없음", 55, 2.4, "흐림"),
                new HourlyWeatherDto.Hour("15:00", 20, rain ? 80 : 10, rain ? "비" : "없음", 55, 2.4, "흐림"),
                new HourlyWeatherDto.Hour("16:00", 20, 10, "없음", 55, 2.4, "구름 많음"),
                new HourlyWeatherDto.Hour("17:00", 19, 10, "없음", 55, 2.4, "구름 많음"),
                new HourlyWeatherDto.Hour("18:00", 18, 10, "없음", 55, 2.4, "맑음"),
                new HourlyWeatherDto.Hour("19:00", 16, 10, "없음", 55, 2.4, "맑음"),
                new HourlyWeatherDto.Hour("23:00", 12, 10, "없음", 55, 2.4, "맑음")
        ), null);
    }
}

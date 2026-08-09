package com.example.WebSideProject.controller;

import com.example.WebSideProject.Enum.AgeGroup;
import com.example.WebSideProject.Enum.ActivityType;
import com.example.WebSideProject.Enum.GenderType;
import com.example.WebSideProject.Enum.TemperatureSensitivity;
import com.example.WebSideProject.Enum.WeatherPeriod;
import com.example.WebSideProject.service.LocationService;
import com.example.WebSideProject.service.RouteBriefingService;
import com.example.WebSideProject.service.WeatherPlannerService;
import com.example.WebSideProject.service.WeatherService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CachePolicyControllerTest {

    @Test
    void weatherPlannerAllowsStaleResponseDuringRefreshOrShortOutage() {
        WeatherController controller = new WeatherController(
                mock(WeatherService.class), mock(WeatherPlannerService.class)
        );

        String cacheControl = controller.getPlanner(
                60, 127, "서울", AgeGroup.NONE, GenderType.NONE,
                TemperatureSensitivity.NONE, ActivityType.DAILY
        ).getHeaders().getCacheControl();

        assertThat(cacheControl)
                .contains("max-age=300")
                .contains("stale-while-revalidate=600")
                .contains("stale-if-error=1800");
    }

    @Test
    void routeBriefingAllowsStaleResponseDuringKakaoOutage() {
        RouteBriefingController controller = new RouteBriefingController(
                mock(RouteBriefingService.class)
        );

        String cacheControl = controller.getBriefing(
                "강남역", "서울시청", WeatherPeriod.MORNING
        ).getHeaders().getCacheControl();

        assertThat(cacheControl)
                .contains("max-age=600")
                .contains("stale-if-error=1800");
    }

    @Test
    void locationSearchUsesLongerStaleFallback() {
        LocationController controller = new LocationController(mock(LocationService.class));

        String cacheControl = controller.search("강남역").getHeaders().getCacheControl();

        assertThat(cacheControl)
                .contains("max-age=1800")
                .contains("stale-while-revalidate=3600")
                .contains("stale-if-error=86400");
    }
}

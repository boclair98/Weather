package com.example.WebSideProject.service;

import com.example.WebSideProject.dto.LocationDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocationServiceTest {

    private final LocationService locationService = new LocationService(null);

    @Test
    void convertsSeoulCoordinatesToKmaGrid() {
        LocationDto.Response response = locationService.resolveCoordinates(37.5665, 126.9780);

        assertThat(response.getLocationName()).isEqualTo("현재 위치");
        assertThat(response.getNx()).isEqualTo(60);
        assertThat(response.getNy()).isEqualTo(127);
    }

    @Test
    void rejectsCoordinatesOutsideKorea() {
        assertThatThrownBy(() -> locationService.resolveCoordinates(35.6762, 139.6503))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("대한민국");
    }
}

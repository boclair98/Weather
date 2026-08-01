package com.example.WebSideProject.controller;

import com.example.WebSideProject.dto.LocationDto;
import com.example.WebSideProject.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/search")
    public ResponseEntity<List<LocationDto.Response>> search(@RequestParam String query) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.MINUTES).cachePublic())
                .body(locationService.search(query));
    }

    @GetMapping("/coordinates")
    public ResponseEntity<LocationDto.Response> resolveCoordinates(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.MINUTES).cachePublic())
                .body(locationService.resolveCoordinates(latitude, longitude));
    }
}

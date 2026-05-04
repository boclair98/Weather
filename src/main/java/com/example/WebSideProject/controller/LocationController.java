package com.example.WebSideProject.controller;

import com.example.WebSideProject.dto.LocationDto;
import com.example.WebSideProject.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/search")
    public List<LocationDto.Response> search(@RequestParam String query) {
        return locationService.search(query);
    }
}

package com.onnoto.onnoto_backend.controller;

import com.onnoto.onnoto_backend.dto.response.StationResponse;
import com.onnoto.onnoto_backend.service.GeospatialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final GeospatialService geospatialService;

    @GetMapping("/nearby")
    public List<StationResponse> getStationsWithinRadius(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false, defaultValue = "5.0") double radiusKm) {

        return geospatialService.getStationsWithinRadius(latitude, longitude, radiusKm);
    }
}
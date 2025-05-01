package com.onnoto.onnoto_backend.controller;



import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.service.StationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
public class StationController {
    private final StationService stationService;

    @GetMapping
    public List<Station> getAllStations() {
        return stationService.getAllStations();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Station> getStationById(@PathVariable String id) {
        return stationService.getStationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/city/{city}")
    public List<Station> getStationsByCity(@PathVariable String city) {
        return stationService.getStationsByCity(city);
    }

    @GetMapping("/nearby")
    public List<Station> getNearbyStations(
            @RequestParam double longitude,
            @RequestParam double latitude,
            @RequestParam(defaultValue = "5000") double radius) {
        return stationService.getNearbyStations(longitude, latitude, radius);
    }
}

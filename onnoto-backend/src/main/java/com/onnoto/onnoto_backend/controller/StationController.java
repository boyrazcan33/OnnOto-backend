package com.onnoto.onnoto_backend.controller;

import com.onnoto.onnoto_backend.dto.request.NearbyRequest;
import com.onnoto.onnoto_backend.dto.request.StationFilterRequest;
import com.onnoto.onnoto_backend.dto.response.StationDetailResponse;
import com.onnoto.onnoto_backend.dto.response.StationResponse;
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
    public List<StationResponse> getAllStations() {
        return stationService.getAllStations();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StationDetailResponse> getStationById(@PathVariable String id) {
        return stationService.getStationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/filter")
    public List<StationResponse> filterStations(@RequestBody StationFilterRequest request) {
        return stationService.filterStations(request);
    }

    @GetMapping("/city/{city}")
    public List<StationResponse> getStationsByCity(@PathVariable String city) {
        StationFilterRequest request = new StationFilterRequest();
        request.setCity(city);
        return stationService.filterStations(request);
    }

    @PostMapping("/nearby")
    public List<StationResponse> getNearbyStations(@RequestBody NearbyRequest request) {
        return stationService.getNearbyStations(request);
    }

    @GetMapping("/nearby")
    public List<StationResponse> getNearbyStationsGet(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false, defaultValue = "5000") double radius,
            @RequestParam(required = false, defaultValue = "10") int limit) {

        NearbyRequest request = new NearbyRequest();
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setRadiusInMeters(radius);
        request.setLimit(limit);

        return stationService.getNearbyStations(request);
    }
}
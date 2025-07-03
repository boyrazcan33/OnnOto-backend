package com.onnoto.onnoto_backend.controller;

import com.onnoto.onnoto_backend.dto.response.ReliabilityResponse;
import com.onnoto.onnoto_backend.service.ReliabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/reliability")
@RequiredArgsConstructor
public class ReliabilityController {

    private final ReliabilityService reliabilityService;

    @GetMapping("/station/{stationId}")
    public ResponseEntity<ReliabilityResponse> getStationReliability(@PathVariable String stationId) {
        return reliabilityService.getStationReliability(stationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/most-reliable")
    public List<ReliabilityResponse> getMostReliableStations(
            @RequestParam(required = false, defaultValue = "10") int limit) {
        return reliabilityService.getMostReliableStations(limit);
    }

    @GetMapping("/minimum-reliability")
    public List<ReliabilityResponse> getStationsWithMinimumReliability(
            @RequestParam BigDecimal minimumUptime) {
        return reliabilityService.getStationsWithMinimumReliability(minimumUptime);
    }

    @GetMapping("/refresh-cache")
    public ResponseEntity<String> refreshCache() {
        reliabilityService.refreshReliabilityData();
        return ResponseEntity.ok("Reliability cache cleared successfully");
    }
}
package com.onnoto.onnoto_backend.controller;

import com.onnoto.onnoto_backend.dto.response.AnomalyResponse;
import com.onnoto.onnoto_backend.model.Anomaly;
import com.onnoto.onnoto_backend.service.AnomalyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyService anomalyService;

    @GetMapping
    public List<AnomalyResponse> getAllAnomalies(
            @RequestParam(required = false) Boolean unresolved,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String type) {

        if (unresolved != null && unresolved) {
            return anomalyService.getUnresolvedAnomalies();
        } else if (severity != null) {
            return anomalyService.getAnomaliesBySeverity(severity);
        } else if (type != null) {
            return anomalyService.getAnomaliesByType(type);
        } else {
            return anomalyService.getAllAnomalies();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnomalyResponse> getAnomalyById(@PathVariable Long id) {
        return anomalyService.getAnomalyById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/station/{stationId}")
    public List<AnomalyResponse> getAnomaliesForStation(
            @PathVariable String stationId,
            @RequestParam(required = false) Boolean unresolved) {

        if (unresolved != null && unresolved) {
            return anomalyService.getUnresolvedAnomaliesForStation(stationId);
        } else {
            return anomalyService.getAnomaliesForStation(stationId);
        }
    }

    @GetMapping("/recent")
    public List<AnomalyResponse> getRecentAnomalies(
            @RequestParam(required = false, defaultValue = "24") Integer hours) {

        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return anomalyService.getAnomaliesSince(since);
    }

    @GetMapping("/stats/problematic-stations")
    public List<Object> getStationsWithMostAnomalies(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {

        return anomalyService.getStationsWithMostAnomalies(limit);
    }
}
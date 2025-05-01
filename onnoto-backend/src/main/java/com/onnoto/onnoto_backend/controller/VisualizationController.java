package com.onnoto.onnoto_backend.controller;

import com.onnoto.onnoto_backend.service.VisualizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/visualizations")
@RequiredArgsConstructor
public class VisualizationController {

    private final VisualizationService visualizationService;

    @GetMapping("/reliability/distribution")
    public Map<String, Object> getReliabilityDistribution() {
        return visualizationService.getReliabilityDistribution();
    }

    @GetMapping("/reliability/networks")
    public List<Map<String, Object>> getReliabilityByNetwork() {
        return visualizationService.getReliabilityByNetwork();
    }

    @GetMapping("/status/history")
    public Map<String, Object> getStatusHistory(
            @RequestParam String stationId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }

        if (endDate == null) {
            endDate = LocalDate.now();
        }

        return visualizationService.getStatusHistory(stationId, startDate, endDate);
    }

    @GetMapping("/anomalies/trends")
    public Map<String, Object> getAnomalyTrends(
            @RequestParam(required = false) Integer days) {

        if (days == null) {
            days = 30;
        }

        return visualizationService.getAnomalyTrends(days);
    }

    @GetMapping("/geographic/heatmap")
    public List<Map<String, Object>> getGeographicHeatmap() {
        return visualizationService.getGeographicHeatmap();
    }

    @GetMapping("/usage/patterns")
    public Map<String, Object> getUsagePatterns(
            @RequestParam(required = false) String stationId) {

        if (stationId != null) {
            return visualizationService.getStationUsagePatterns(stationId);
        } else {
            return visualizationService.getOverallUsagePatterns();
        }
    }
}
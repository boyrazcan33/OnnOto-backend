package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.dto.response.AnomalyResponse;
import com.onnoto.onnoto_backend.model.Anomaly;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.repository.AnomalyRepository;
import com.onnoto.onnoto_backend.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyService {

    private final AnomalyRepository anomalyRepository;
    private final StationRepository stationRepository;

    @Cacheable(value = "anomalies")
    @Transactional(readOnly = true)
    public List<AnomalyResponse> getAllAnomalies() {
        return anomalyRepository.findAll().stream()
                .map(this::convertToAnomalyResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "anomalies", key = "'unresolved'")
    @Transactional(readOnly = true)
    public List<AnomalyResponse> getUnresolvedAnomalies() {
        return anomalyRepository.findByIsResolvedFalse().stream()
                .map(this::convertToAnomalyResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "anomalies", key = "'station-' + #stationId")
    @Transactional(readOnly = true)
    public List<AnomalyResponse> getAnomaliesForStation(String stationId) {
        return stationRepository.findById(stationId)
                .map(station -> anomalyRepository.findByStation(station).stream()
                        .map(this::convertToAnomalyResponse)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Cacheable(value = "anomalies", key = "'station-unresolved-' + #stationId")
    @Transactional(readOnly = true)
    public List<AnomalyResponse> getUnresolvedAnomaliesForStation(String stationId) {
        return stationRepository.findById(stationId)
                .map(station -> anomalyRepository.findByStationAndIsResolvedFalse(station).stream()
                        .map(this::convertToAnomalyResponse)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Cacheable(value = "anomalies", key = "'id-' + #id")
    @Transactional(readOnly = true)
    public Optional<AnomalyResponse> getAnomalyById(Long id) {
        return anomalyRepository.findById(id)
                .map(this::convertToAnomalyResponse);
    }

    @Cacheable(value = "anomalies", key = "'type-' + #type")
    @Transactional(readOnly = true)
    public List<AnomalyResponse> getAnomaliesByType(String type) {
        try {
            Anomaly.AnomalyType anomalyType = Anomaly.AnomalyType.valueOf(type.toUpperCase());
            return anomalyRepository.findByAnomalyType(anomalyType).stream()
                    .map(this::convertToAnomalyResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid anomaly type: {}", type);
            return List.of();
        }
    }

    @Cacheable(value = "anomalies", key = "'severity-' + #severity")
    @Transactional(readOnly = true)
    public List<AnomalyResponse> getAnomaliesBySeverity(String severity) {
        try {
            Anomaly.AnomalySeverity anomalySeverity = Anomaly.AnomalySeverity.valueOf(severity.toUpperCase());
            return anomalyRepository.findBySeverity(anomalySeverity).stream()
                    .map(this::convertToAnomalyResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid anomaly severity: {}", severity);
            return List.of();
        }
    }

    @Cacheable(value = "anomalies", key = "'since-' + #since.toString()")
    @Transactional(readOnly = true)
    public List<AnomalyResponse> getAnomaliesSince(LocalDateTime since) {
        return anomalyRepository.findRecentAnomalies(since).stream()
                .map(this::convertToAnomalyResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "anomalies", key = "'problematic-' + #limit")
    @Transactional(readOnly = true)
    public List<Object> getStationsWithMostAnomalies(int limit) {
        List<Object[]> results = anomalyRepository.findStationsWithMostAnomalies(
                PageRequest.of(0, limit));

        List<Object> response = new ArrayList<>();
        for (Object[] result : results) {
            Station station = (Station) result[0];
            Long count = (Long) result[1];

            Map<String, Object> item = new HashMap<>();
            item.put("stationId", station.getId());
            item.put("stationName", station.getName());
            item.put("networkName", station.getNetwork() != null ? station.getNetwork().getName() : null);
            item.put("anomalyCount", count);

            response.add(item);
        }

        return response;
    }

    private AnomalyResponse convertToAnomalyResponse(Anomaly anomaly) {
        AnomalyResponse response = new AnomalyResponse();
        response.setId(anomaly.getId());
        response.setStationId(anomaly.getStation().getId());
        response.setStationName(anomaly.getStation().getName());
        response.setAnomalyType(anomaly.getAnomalyType().name());
        response.setDescription(anomaly.getDescription());
        response.setSeverity(anomaly.getSeverity().name());
        response.setSeverityScore(anomaly.getSeverityScore());
        response.setResolved(anomaly.getIsResolved());
        response.setDetectedAt(anomaly.getDetectedAt());
        response.setResolvedAt(anomaly.getResolvedAt());
        response.setLastChecked(anomaly.getLastChecked());
        return response;
    }
}
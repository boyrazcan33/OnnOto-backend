package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.dto.response.ReliabilityResponse;
import com.onnoto.onnoto_backend.model.ReliabilityMetric;
import com.onnoto.onnoto_backend.repository.ReliabilityMetricRepository;
import com.onnoto.onnoto_backend.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReliabilityService {
    private final ReliabilityMetricRepository reliabilityMetricRepository;
    private final StationRepository stationRepository;

    /**
     * Get reliability metrics for a specific station
     */
    @Cacheable(value = "reliability", key = "#stationId", unless = "#result == null")
    @Transactional(readOnly = true)
    public Optional<ReliabilityResponse> getStationReliability(String stationId) {
        log.debug("Fetching reliability metrics for station: {}", stationId);
        return stationRepository.findById(stationId)
                .flatMap(reliabilityMetricRepository::findByStation)
                .map(this::convertToReliabilityResponse);
    }

    /**
     * Get a list of the most reliable stations
     */
    @Cacheable(value = "reliability", key = "'mostReliable-' + #limit")
    @Transactional(readOnly = true)
    public List<ReliabilityResponse> getMostReliableStations(int limit) {
        log.debug("Fetching top {} most reliable stations", limit);
        return reliabilityMetricRepository.findAllOrderByUptimePercentageDesc().stream()
                .limit(limit)
                .map(this::convertToReliabilityResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get stations with a minimum reliability score
     */
    @Cacheable(value = "reliability", key = "'minReliability-' + #minimumUptime")
    @Transactional(readOnly = true)
    public List<ReliabilityResponse> getStationsWithMinimumReliability(BigDecimal minimumUptime) {
        log.debug("Fetching stations with minimum reliability: {}", minimumUptime);
        return reliabilityMetricRepository.findAllWithMinimumUptime(minimumUptime).stream()
                .map(this::convertToReliabilityResponse)
                .collect(Collectors.toList());
    }

    /**
     * Clear reliability-related caches after recalculations
     */
    @CacheEvict(value = "reliability", allEntries = true)
    @Transactional
    public void refreshReliabilityData() {
        log.info("Refreshed reliability data caches");
    }

    private ReliabilityResponse convertToReliabilityResponse(ReliabilityMetric metric) {
        ReliabilityResponse response = new ReliabilityResponse();
        response.setStationId(metric.getStation().getId());
        response.setUptimePercentage(metric.getUptimePercentage());
        response.setReportCount(metric.getReportCount());
        response.setAverageReportSeverity(metric.getAverageReportSeverity());
        response.setLastDowntime(metric.getLastDowntime());
        response.setDowntimeFrequency(metric.getDowntimeFrequency());
        response.setSampleSize(metric.getSampleSize());
        return response;
    }
}
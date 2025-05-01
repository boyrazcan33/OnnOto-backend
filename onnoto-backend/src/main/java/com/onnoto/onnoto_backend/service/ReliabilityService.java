package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.dto.response.ReliabilityResponse;
import com.onnoto.onnoto_backend.model.ReliabilityMetric;
import com.onnoto.onnoto_backend.repository.ReliabilityMetricRepository;
import com.onnoto.onnoto_backend.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReliabilityService {
    private final ReliabilityMetricRepository reliabilityMetricRepository;
    private final StationRepository stationRepository;

    @Transactional(readOnly = true)
    public Optional<ReliabilityResponse> getStationReliability(String stationId) {
        return stationRepository.findById(stationId)
                .flatMap(reliabilityMetricRepository::findByStation)
                .map(this::convertToReliabilityResponse);
    }

    @Transactional(readOnly = true)
    public List<ReliabilityResponse> getMostReliableStations(int limit) {
        return reliabilityMetricRepository.findAllOrderByUptimePercentageDesc().stream()
                .limit(limit)
                .map(this::convertToReliabilityResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReliabilityResponse> getStationsWithMinimumReliability(BigDecimal minimumUptime) {
        return reliabilityMetricRepository.findAllWithMinimumUptime(minimumUptime).stream()
                .map(this::convertToReliabilityResponse)
                .collect(Collectors.toList());
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
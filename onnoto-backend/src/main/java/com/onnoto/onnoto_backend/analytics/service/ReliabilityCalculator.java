package com.onnoto.onnoto_backend.analytics.service;

import com.onnoto.onnoto_backend.model.ReliabilityMetric;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.repository.ConnectorRepository;
import com.onnoto.onnoto_backend.repository.ReliabilityMetricRepository;
import com.onnoto.onnoto_backend.repository.ReportRepository;
import com.onnoto.onnoto_backend.repository.StatusHistoryRepository;
import com.onnoto.onnoto_backend.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReliabilityCalculator {

    private final StationRepository stationRepository;
    private final ConnectorRepository connectorRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final ReportRepository reportRepository;
    private final ReliabilityMetricRepository reliabilityMetricRepository;

    private static final int ANALYSIS_PERIOD_DAYS = 30;
    private static final int MIN_DATA_POINTS = 10;
    private static final double WEIGHT_UPTIME = 0.6;
    private static final double WEIGHT_STABILITY = 0.2;
    private static final double WEIGHT_REPORTS = 0.2;

    /**
     * Calculate reliability scores for all stations
     */
    @Transactional
    public void calculateAllStationReliability() {
        log.info("Starting reliability calculation for all stations");
        List<Station> stations = stationRepository.findAll();

        for (Station station : stations) {
            try {
                calculateStationReliability(station);
            } catch (Exception e) {
                log.error("Error calculating reliability for station {}: {}",
                        station.getId(), e.getMessage(), e);
            }
        }

        log.info("Completed reliability calculation for {} stations", stations.size());
    }

    /**
     * Calculate reliability score for a single station
     */
    @Transactional
    public void calculateStationReliability(Station station) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minus(ANALYSIS_PERIOD_DAYS, ChronoUnit.DAYS);

        // Get the station's history data
        List<StatusHistoryRepository.StatusCountDto> statusCounts =
                statusHistoryRepository.countStatusesByStation(station.getId(), startDate, now);

        // Calculate uptime percentage
        BigDecimal uptimePercentage = calculateUptimePercentage(statusCounts);

        // Calculate status stability (fewer changes is better)
        BigDecimal statusStability = calculateStatusStability(station.getId(), startDate, now);

        // Calculate report score (fewer reports is better)
        BigDecimal reportScore = calculateReportScore(station.getId(), startDate, now);

        // Calculate data confidence
        int sampleSize = statusCounts.stream()
                .mapToInt(StatusHistoryRepository.StatusCountDto::getCount)
                .sum();

        // Calculate overall score with weighting
        BigDecimal reliabilityScore = calculateWeightedScore(
                uptimePercentage, statusStability, reportScore, sampleSize);

        // Update or create reliability metric
        updateReliabilityMetric(station, uptimePercentage, reportScore,
                statusStability, sampleSize, reliabilityScore);

        // Update station with overall score
        updateStationReliabilityScore(station, reliabilityScore);

        log.info("Calculated reliability score {} for station {}",
                reliabilityScore, station.getId());
    }

    private BigDecimal calculateUptimePercentage(List<StatusHistoryRepository.StatusCountDto> statusCounts) {
        // Implementation will depend on your status types and classification
        // This is a simplified example
        int totalCount = statusCounts.stream()
                .mapToInt(StatusHistoryRepository.StatusCountDto::getCount)
                .sum();

        if (totalCount == 0) {
            return BigDecimal.valueOf(50.0); // Default for no data
        }

        int availableCount = statusCounts.stream()
                .filter(dto -> "AVAILABLE".equals(dto.getStatus()))
                .mapToInt(StatusHistoryRepository.StatusCountDto::getCount)
                .sum();

        return BigDecimal.valueOf(availableCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateStatusStability(String stationId,
                                                LocalDateTime startDate, LocalDateTime endDate) {
        // Count status transitions
        long transitionCount = statusHistoryRepository.countStatusTransitions(stationId, startDate, endDate);

        // Convert to a score (0-100)
        // More transitions = lower score
        // Example: 0 transitions = 100, 20+ transitions = 0
        long maxTransitions = 20;
        long cappedTransitions = Math.min(transitionCount, maxTransitions);

        return BigDecimal.valueOf(100)
                .subtract(BigDecimal.valueOf(cappedTransitions)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(maxTransitions), 2, RoundingMode.HALF_UP));
    }

    private BigDecimal calculateReportScore(String stationId,
                                            LocalDateTime startDate, LocalDateTime endDate) {
        // Count reports
        long reportCount = reportRepository.countByStationIdAndDateRange(stationId, startDate, endDate);

        // Convert to a score (0-100)
        // More reports = lower score
        // Example: 0 reports = 100, 10+ reports = 0
        long maxReports = 10;
        long cappedReports = Math.min(reportCount, maxReports);

        return BigDecimal.valueOf(100)
                .subtract(BigDecimal.valueOf(cappedReports)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(maxReports), 2, RoundingMode.HALF_UP));
    }

    private BigDecimal calculateWeightedScore(BigDecimal uptimePercentage,
                                              BigDecimal statusStability, BigDecimal reportScore, int sampleSize) {

        // Apply confidence factor based on sample size
        BigDecimal confidenceFactor = calculateConfidenceFactor(sampleSize);

        // Calculate weighted score
        BigDecimal weightedScore =
                uptimePercentage.multiply(BigDecimal.valueOf(WEIGHT_UPTIME))
                        .add(statusStability.multiply(BigDecimal.valueOf(WEIGHT_STABILITY)))
                        .add(reportScore.multiply(BigDecimal.valueOf(WEIGHT_REPORTS)));

        // Apply confidence factor
        return weightedScore.multiply(confidenceFactor)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateConfidenceFactor(int sampleSize) {
        if (sampleSize >= MIN_DATA_POINTS) {
            return BigDecimal.ONE; // Full confidence
        }

        // Partial confidence
        return BigDecimal.valueOf(sampleSize)
                .divide(BigDecimal.valueOf(MIN_DATA_POINTS), 2, RoundingMode.HALF_UP)
                .add(BigDecimal.valueOf(0.5)); // Base confidence of 0.5
    }

    private void updateReliabilityMetric(Station station, BigDecimal uptimePercentage,
                                         BigDecimal reportScore, BigDecimal statusStability, int sampleSize,
                                         BigDecimal reliabilityScore) {

        ReliabilityMetric metric = reliabilityMetricRepository.findByStation(station)
                .orElse(new ReliabilityMetric());

        metric.setStation(station);
        metric.setUptimePercentage(uptimePercentage);
        metric.setReportCount(reportRepository.countByStation(station));
        // Calculate average report severity (would require additional data)
        metric.setDowntimeFrequency(BigDecimal.valueOf(100).subtract(statusStability));
        metric.setSampleSize(sampleSize);
        metric.setUpdatedAt(LocalDateTime.now());

        if (metric.getCreatedAt() == null) {
            metric.setCreatedAt(LocalDateTime.now());
        }

        reliabilityMetricRepository.save(metric);
    }

    private void updateStationReliabilityScore(Station station, BigDecimal reliabilityScore) {
        station.setReliabilityScore(reliabilityScore);
        station.setUpdatedAt(LocalDateTime.now());
        stationRepository.save(station);
    }
}
package com.onnoto.onnoto_backend.analytics.service;

import com.onnoto.onnoto_backend.model.Anomaly;
import com.onnoto.onnoto_backend.model.Connector;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.model.StatusHistory;
import com.onnoto.onnoto_backend.repository.AnomalyRepository;
import com.onnoto.onnoto_backend.repository.ConnectorRepository;
import com.onnoto.onnoto_backend.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatternDeviationDetector {

    private final ConnectorRepository connectorRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final AnomalyRepository anomalyRepository;

    // Constants for configuration
    private static final int ANALYSIS_WINDOW_DAYS = 14;  // Look at last 14 days
    private static final double DEVIATION_THRESHOLD = 0.3;  // 30% deviation from pattern is anomalous

    /**
     * Detect deviations from normal usage patterns
     * @return number of anomalies detected
     */
    public int detect(Station station) {
        log.debug("Checking for pattern deviations at station: {}", station.getId());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minus(ANALYSIS_WINDOW_DAYS, ChronoUnit.DAYS);

        List<Connector> connectors = connectorRepository.findByStation(station);
        int anomaliesDetected = 0;

        // We'll analyze by day of week and time of day
        // Build expected patterns, then check recent days against them

        // Step 1: Build historical day-of-week patterns
        Map<DayOfWeek, Map<String, Integer>> dayOfWeekPatterns = new HashMap<>();

        // Step 2: Get recent status history
        for (Connector connector : connectors) {
            List<StatusHistory> history = statusHistoryRepository.findByConnectorAndRecordedAtBetween(
                    connector, startTime, now);

            // Skip if not enough data
            if (history.size() < 10) {
                continue;
            }

            // Step 3: Build historical patterns
            Map<DayOfWeek, Map<String, Integer>> expectedPattern = buildExpectedPattern(history);

            // Step 4: Check recent days against pattern
            boolean patternDeviation = checkPatternDeviation(connector, history, expectedPattern);

            if (patternDeviation) {
                createPatternDeviationAnomaly(station, connector);
                anomaliesDetected++;
            }
        }

        return anomaliesDetected;
    }

    private Map<DayOfWeek, Map<String, Integer>> buildExpectedPattern(List<StatusHistory> history) {
        Map<DayOfWeek, Map<String, Integer>> dayPatterns = new HashMap<>();

        // Initialize all days and statuses
        for (DayOfWeek day : DayOfWeek.values()) {
            Map<String, Integer> statusCounts = new HashMap<>();
            statusCounts.put("AVAILABLE", 0);
            statusCounts.put("OCCUPIED", 0);
            statusCounts.put("OFFLINE", 0);
            dayPatterns.put(day, statusCounts);
        }

        // Count statuses by day of week
        for (StatusHistory record : history) {
            DayOfWeek day = record.getRecordedAt().getDayOfWeek();
            String status = record.getStatus();

            Map<String, Integer> dayCounts = dayPatterns.get(day);
            dayCounts.put(status, dayCounts.getOrDefault(status, 0) + 1);
        }

        return dayPatterns;
    }

    private boolean checkPatternDeviation(
            Connector connector, List<StatusHistory> history,
            Map<DayOfWeek, Map<String, Integer>> expectedPattern) {

        // For simplicity, we'll check if the recent pattern differs
        // significantly from the expected pattern

        // Get most recent status for this connector
        String currentStatus = connector.getStatus();

        // Check if current status deviates from expected for current day
        DayOfWeek today = LocalDateTime.now().getDayOfWeek();
        Map<String, Integer> expectedToday = expectedPattern.get(today);

        // Find most common status
        String expectedStatus = findMostCommonStatus(expectedToday);

        // Simple check - if current status differs from most common for this day,
        // and we have enough data, it might be a deviation
        return !currentStatus.equals(expectedStatus) &&
                sumStatusCounts(expectedToday) >= 10;
    }

    private String findMostCommonStatus(Map<String, Integer> statusCounts) {
        String mostCommon = "AVAILABLE"; // Default
        int maxCount = 0;

        for (Map.Entry<String, Integer> entry : statusCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostCommon = entry.getKey();
            }
        }

        return mostCommon;
    }

    private int sumStatusCounts(Map<String, Integer> statusCounts) {
        return statusCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    private void createPatternDeviationAnomaly(Station station, Connector connector) {
        // Check if there's already an unresolved pattern deviation anomaly
        List<Anomaly> existingAnomalies = anomalyRepository.findByStationAndIsResolvedFalse(station);

        for (Anomaly existing : existingAnomalies) {
            if (existing.getAnomalyType() == Anomaly.AnomalyType.PATTERN_DEVIATION) {
                // Update the existing anomaly
                existing.setLastChecked(LocalDateTime.now());

                // Update description
                existing.setDescription(String.format(
                        "Unusual status pattern detected for connector %d: expected pattern deviation",
                        connector.getId()));

                anomalyRepository.save(existing);
                return;
            }
        }

        // Create new anomaly if none exists
        Anomaly anomaly = new Anomaly();
        anomaly.setStation(station);
        anomaly.setAnomalyType(Anomaly.AnomalyType.PATTERN_DEVIATION);
        anomaly.setDescription(String.format(
                "Unusual status pattern detected for connector %d: expected pattern deviation",
                connector.getId()));

        // In a real-world implementation, we would calculate a more sophisticated
        // deviation score here
        anomaly.setSeverity(Anomaly.AnomalySeverity.MEDIUM);
        anomaly.setSeverityScore(BigDecimal.valueOf(0.5));

        anomaly.setDetectedAt(LocalDateTime.now());
        anomaly.setLastChecked(LocalDateTime.now());
        anomaly.setIsResolved(false);

        anomalyRepository.save(anomaly);
        log.info("Created new PATTERN_DEVIATION anomaly for station: {}", station.getId());
    }
}
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtendedDowntimeDetector {

    private final ConnectorRepository connectorRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final AnomalyRepository anomalyRepository;

    // Constants for configuration
    private static final int ANALYSIS_WINDOW_HOURS = 72;  // Look at last 72 hours
    private static final int DOWNTIME_THRESHOLD_HOURS = 24;  // 24+ hours offline is anomalous

    /**
     * Detect extended downtime for a station
     * @return number of anomalies detected
     */
    public int detect(Station station) {
        log.debug("Checking for extended downtime at station: {}", station.getId());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minus(ANALYSIS_WINDOW_HOURS, ChronoUnit.HOURS);

        List<Connector> connectors = connectorRepository.findByStation(station);
        int anomaliesDetected = 0;

        // Check each connector for extended downtime
        for (Connector connector : connectors) {
            if ("OFFLINE".equals(connector.getStatus())) {
                // Get the most recent status changes
                List<StatusHistory> history = statusHistoryRepository.findByConnectorAndRecordedAtBetween(
                        connector, startTime, now);

                // Sort by time, newest first
                history.sort(Comparator.comparing(StatusHistory::getRecordedAt).reversed());

                // Find when it went offline
                Optional<StatusHistory> offlineEvent = history.stream()
                        .filter(h -> "OFFLINE".equals(h.getStatus()))
                        .findFirst();

                if (offlineEvent.isPresent()) {
                    LocalDateTime offlineTime = offlineEvent.get().getRecordedAt();
                    Duration downtime = Duration.between(offlineTime, now);

                    if (downtime.toHours() >= DOWNTIME_THRESHOLD_HOURS) {
                        createDowntimeAnomaly(station, connector, downtime.toHours());
                        anomaliesDetected++;
                    }
                }
            }
        }

        return anomaliesDetected;
    }

    private void createDowntimeAnomaly(Station station, Connector connector, long downtimeHours) {
        // Check if there's already an unresolved downtime anomaly
        List<Anomaly> existingAnomalies = anomalyRepository.findByStationAndIsResolvedFalse(station);

        for (Anomaly existing : existingAnomalies) {
            if (existing.getAnomalyType() == Anomaly.AnomalyType.EXTENDED_DOWNTIME) {
                // Update the existing anomaly
                existing.setLastChecked(LocalDateTime.now());

                // Update description with latest count
                existing.setDescription(String.format(
                        "Connector %d has been offline for %d hours",
                        connector.getId(), downtimeHours));

                // Calculate severity based on downtime length
                int severityLevel = calculateSeverity(downtimeHours);
                if (severityLevel > getSeverityValue(existing.getSeverity())) {
                    existing.setSeverity(getSeverityFromValue(severityLevel));
                }

                anomalyRepository.save(existing);
                return;
            }
        }

        // Create new anomaly if none exists
        Anomaly anomaly = new Anomaly();
        anomaly.setStation(station);
        anomaly.setAnomalyType(Anomaly.AnomalyType.EXTENDED_DOWNTIME);
        anomaly.setDescription(String.format(
                "Connector %d has been offline for %d hours",
                connector.getId(), downtimeHours));

        // Set severity based on downtime length
        int severityLevel = calculateSeverity(downtimeHours);
        anomaly.setSeverity(getSeverityFromValue(severityLevel));
        anomaly.setSeverityScore(BigDecimal.valueOf(downtimeHours));

        anomaly.setDetectedAt(LocalDateTime.now());
        anomaly.setLastChecked(LocalDateTime.now());
        anomaly.setIsResolved(false);

        anomalyRepository.save(anomaly);
        log.info("Created new EXTENDED_DOWNTIME anomaly for station: {}", station.getId());
    }

    private int calculateSeverity(long downtimeHours) {
        if (downtimeHours >= 72) {
            return 3; // HIGH
        } else if (downtimeHours >= 48) {
            return 2; // MEDIUM
        } else {
            return 1; // LOW
        }
    }

    private int getSeverityValue(Anomaly.AnomalySeverity severity) {
        switch (severity) {
            case HIGH:
                return 3;
            case MEDIUM:
                return 2;
            case LOW:
            default:
                return 1;
        }
    }

    private Anomaly.AnomalySeverity getSeverityFromValue(int value) {
        switch (value) {
            case 3:
                return Anomaly.AnomalySeverity.HIGH;
            case 2:
                return Anomaly.AnomalySeverity.MEDIUM;
            case 1:
            default:
                return Anomaly.AnomalySeverity.LOW;
        }
    }
}
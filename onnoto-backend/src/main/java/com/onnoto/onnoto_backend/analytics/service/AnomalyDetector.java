package com.onnoto.onnoto_backend.analytics.service;

import com.onnoto.onnoto_backend.model.Anomaly;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.repository.AnomalyRepository;
import com.onnoto.onnoto_backend.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyDetector {

    private final StationRepository stationRepository;
    private final AnomalyRepository anomalyRepository;
    private final StatusFlappingDetector statusFlappingDetector;
    private final ExtendedDowntimeDetector extendedDowntimeDetector;
    private final ReportSpikeDetector reportSpikeDetector;
    private final PatternDeviationDetector patternDeviationDetector;

    /**
     * Run all anomaly detection algorithms on all stations
     */
    public void detectAnomalies() {
        log.info("Starting anomaly detection for all stations");
        List<Station> stations = stationRepository.findAll();

        int detectedAnomalies = 0;

        for (Station station : stations) {
            try {
                detectedAnomalies += detectAnomaliesForStation(station);
            } catch (Exception e) {
                log.error("Error detecting anomalies for station {}: {}",
                        station.getId(), e.getMessage(), e);
            }
        }

        log.info("Completed anomaly detection. Found {} anomalies across {} stations",
                detectedAnomalies, stations.size());
    }

    /**
     * Run all anomaly detection algorithms on a specific station
     */
    public int detectAnomaliesForStation(Station station) {
        log.debug("Detecting anomalies for station: {}", station.getId());

        int detectedAnomalies = 0;

        // Check for status flapping (rapid status changes)
        detectedAnomalies += statusFlappingDetector.detect(station);

        // Check for extended downtime
        detectedAnomalies += extendedDowntimeDetector.detect(station);

        // Check for report spikes
        detectedAnomalies += reportSpikeDetector.detect(station);

        // Check for pattern deviations
        detectedAnomalies += patternDeviationDetector.detect(station);

        return detectedAnomalies;
    }

    /**
     * Update reliability scores based on anomalies
     */
    public void updateReliabilityScoresFromAnomalies() {
        // Implementation would adjust reliability scores
        // based on active anomalies
    }

    /**
     * Check if previously detected anomalies have been resolved
     */
    public void checkForResolvedAnomalies() {
        List<Anomaly> unresolvedAnomalies = anomalyRepository.findByIsResolvedFalse();
        log.info("Checking {} unresolved anomalies for resolution", unresolvedAnomalies.size());

        int resolvedCount = 0;

        for (Anomaly anomaly : unresolvedAnomalies) {
            boolean isResolved = checkIfAnormalyIsResolved(anomaly);

            if (isResolved) {
                anomaly.setIsResolved(true);
                anomaly.setResolvedAt(LocalDateTime.now());
                anomalyRepository.save(anomaly);
                resolvedCount++;
            }
        }

        log.info("Marked {} anomalies as resolved", resolvedCount);
    }

    private boolean checkIfAnormalyIsResolved(Anomaly anomaly) {
        // Logic to determine if an anomaly is resolved would depend
        // on the type of anomaly and current station status
        return false; // Placeholder
    }
}
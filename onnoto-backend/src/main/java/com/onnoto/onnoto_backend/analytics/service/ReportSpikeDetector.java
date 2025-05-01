package com.onnoto.onnoto_backend.analytics.service;

import com.onnoto.onnoto_backend.model.Anomaly;
import com.onnoto.onnoto_backend.model.Report;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.repository.AnomalyRepository;
import com.onnoto.onnoto_backend.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportSpikeDetector {

    private final ReportRepository reportRepository;
    private final AnomalyRepository anomalyRepository;

    // Constants for configuration
    private static final int ANALYSIS_WINDOW_DAYS = 7;  // Look at last 7 days
    private static final int COMPARISON_WINDOW_DAYS = 30;  // Compare with the last 30 days
    private static final double SPIKE_THRESHOLD = 2.0;  // 2x normal rate is a spike
    private static final int MIN_REPORTS = 3;  // Need at least this many reports to detect a spike

    /**
     * Detect spikes in user reports for a station
     * @return number of anomalies detected
     */
    public int detect(Station station) {
        log.debug("Checking for report spikes at station: {}", station.getId());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recentPeriodStart = now.minus(ANALYSIS_WINDOW_DAYS, ChronoUnit.DAYS);
        LocalDateTime historyPeriodStart = now.minus(COMPARISON_WINDOW_DAYS, ChronoUnit.DAYS);

        // Get recent reports
        List<Report> recentReports = reportRepository.findByStationAndCreatedAtBetween(
                station, recentPeriodStart, now);

        // Get historical reports (excluding recent period)
        List<Report> historicalReports = reportRepository.findByStationAndCreatedAtBetween(
                station, historyPeriodStart, recentPeriodStart);

        // Count by report type
        Map<String, Integer> recentTypeCounts = countReportsByType(recentReports);
        Map<String, Integer> historicalTypeCounts = countReportsByType(historicalReports);

        // Check for spikes
        int anomaliesDetected = 0;
        for (Map.Entry<String, Integer> entry : recentTypeCounts.entrySet()) {
            String reportType = entry.getKey();
            int recentCount = entry.getValue();

            // Only consider if we have enough reports
            if (recentCount >= MIN_REPORTS) {
                // Calculate rate per day
                double recentDailyRate = (double) recentCount / ANALYSIS_WINDOW_DAYS;

                // Get historical count for this type
                int historicalCount = historicalTypeCounts.getOrDefault(reportType, 0);

                // Calculate historical daily rate (avoid division by zero)
                double historicalDaysSpan = COMPARISON_WINDOW_DAYS - ANALYSIS_WINDOW_DAYS;
                double historicalDailyRate = historicalDaysSpan > 0
                        ? (double) historicalCount / historicalDaysSpan
                        : 0;

                // Detect spike - if recent rate is much higher than historical
                if (historicalDailyRate > 0 && recentDailyRate >= SPIKE_THRESHOLD * historicalDailyRate) {
                    createReportSpikeAnomaly(station, reportType, recentCount, recentDailyRate, historicalDailyRate);
                    anomaliesDetected++;
                }
                // Also detect if there were no historical reports but suddenly many recent ones
                else if (historicalCount == 0 && recentCount >= MIN_REPORTS * 2) {
                    createReportSpikeAnomaly(station, reportType, recentCount, recentDailyRate, 0);
                    anomaliesDetected++;
                }
            }
        }

        return anomaliesDetected;
    }

    private Map<String, Integer> countReportsByType(List<Report> reports) {
        Map<String, Integer> typeCounts = new HashMap<>();

        for (Report report : reports) {
            String reportType = report.getReportType();
            typeCounts.put(reportType, typeCounts.getOrDefault(reportType, 0) + 1);
        }

        return typeCounts;
    }

    private void createReportSpikeAnomaly(Station station, String reportType,
                                          int reportCount, double recentRate, double historicalRate) {

        // Check if there's already an unresolved report spike anomaly for this type
        List<Anomaly> existingAnomalies = anomalyRepository.findByStationAndIsResolvedFalse(station);

        for (Anomaly existing : existingAnomalies) {
            if (existing.getAnomalyType() == Anomaly.AnomalyType.REPORT_SPIKE &&
                    existing.getDescription().contains(reportType)) {
                // Update the existing anomaly
                existing.setLastChecked(LocalDateTime.now());

                // Update description with latest count
                existing.setDescription(String.format(
                        "Spike in '%s' reports: %d reports in last %d days (%.1f per day vs. historical %.1f per day)",
                        reportType, reportCount, ANALYSIS_WINDOW_DAYS, recentRate, historicalRate));

                // Calculate severity based on spike magnitude
                double spikeFactor = historicalRate > 0 ? recentRate / historicalRate : reportCount;
                BigDecimal severityScore = BigDecimal.valueOf(spikeFactor);
                existing.setSeverityScore(severityScore);

                int severityLevel = calculateSeverity(spikeFactor);
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
        anomaly.setAnomalyType(Anomaly.AnomalyType.REPORT_SPIKE);
        anomaly.setDescription(String.format(
                "Spike in '%s' reports: %d reports in last %d days (%.1f per day vs. historical %.1f per day)",
                reportType, reportCount, ANALYSIS_WINDOW_DAYS, recentRate, historicalRate));

        // Set severity based on spike magnitude
        double spikeFactor = historicalRate > 0 ? recentRate / historicalRate : reportCount;
        anomaly.setSeverityScore(BigDecimal.valueOf(spikeFactor));

        int severityLevel = calculateSeverity(spikeFactor);
        anomaly.setSeverity(getSeverityFromValue(severityLevel));

        anomaly.setDetectedAt(LocalDateTime.now());
        anomaly.setLastChecked(LocalDateTime.now());
        anomaly.setIsResolved(false);

        anomalyRepository.save(anomaly);
        log.info("Created new REPORT_SPIKE anomaly for station: {}", station.getId());
    }

    private int calculateSeverity(double spikeFactor) {
        if (spikeFactor >= 5.0) {
            return 3; // HIGH - 5x or more increase
        } else if (spikeFactor >= 3.0) {
            return 2; // MEDIUM - 3-5x increase
        } else {
            return 1; // LOW - 2-3x increase
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
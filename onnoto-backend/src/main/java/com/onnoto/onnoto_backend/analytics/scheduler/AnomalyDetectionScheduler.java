package com.onnoto.onnoto_backend.analytics.scheduler;

import com.onnoto.onnoto_backend.analytics.service.AnomalyDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnomalyDetectionScheduler {

    private final AnomalyDetector anomalyDetector;

    /**
     * Run anomaly detection every 2 hours
     */
    @Scheduled(cron = "0 0 */2 * * ?")
    public void detectAnomalies() {
        log.info("Running scheduled anomaly detection");
        anomalyDetector.detectAnomalies();
    }

    /**
     * Check for resolved anomalies every hour
     */
    @Scheduled(cron = "0 30 * * * ?")
    public void checkForResolvedAnomalies() {
        log.info("Checking for resolved anomalies");
        anomalyDetector.checkForResolvedAnomalies();
    }

    /**
     * Update reliability scores based on anomalies once a day
     */
    @Scheduled(cron = "0 0 4 * * ?")  // 4 AM daily
    public void updateReliabilityFromAnomalies() {
        log.info("Updating reliability scores from anomalies");
        anomalyDetector.updateReliabilityScoresFromAnomalies();
    }
}
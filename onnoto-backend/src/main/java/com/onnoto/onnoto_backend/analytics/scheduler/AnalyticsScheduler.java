package com.onnoto.onnoto_backend.analytics.scheduler;

import com.onnoto.onnoto_backend.analytics.service.ReliabilityCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class AnalyticsScheduler {

    private final ReliabilityCalculator reliabilityCalculator;

    /**
     * Calculate reliability scores daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void calculateReliability() {
        log.info("Starting scheduled reliability score calculation");
        reliabilityCalculator.calculateAllStationReliability();
    }

    /**
     * Initial calculation on startup (after a delay to allow data ingestion)
     */
    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE)
    public void initialReliabilityCalculation() {
        log.info("Starting initial reliability score calculation");
        reliabilityCalculator.calculateAllStationReliability();
    }
}
package com.onnoto.onnoto_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final CacheManager cacheManager;
    private final StationService stationService;
    private final ConnectorService connectorService;
    private final ReliabilityService reliabilityService;
    private final PreferenceService preferenceService;
    private final ReportService reportService;
    private final AnonymousUserService anonymousUserService;

    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        log.info("Clearing all application caches");
        cacheManager.getCacheNames().forEach(cacheName -> {
            Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
            log.debug("Cleared cache: {}", cacheName);
        });
    }

    /**
     * Clear specific caches
     */
    public void clearCaches(String... cacheNames) {
        log.info("Clearing specific caches");
        for (String cacheName : cacheNames) {
            Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
            log.debug("Cleared cache: {}", cacheName);
        }
    }

    /**
     * Clear only status-related caches after status updates
     */
    public void clearStatusCaches() {
        log.info("Clearing status-related caches");
        stationService.refreshStationData();
        connectorService.refreshConnectorData();
    }

    /**
     * Clear reliability caches after reliability calculation
     */
    public void clearReliabilityCaches() {
        log.info("Clearing reliability caches");
        reliabilityService.refreshReliabilityData();
        stationService.refreshStationData(); // Station contains reliability score
    }

    /**
     * Clear user preference caches
     */
    public void clearPreferenceCaches() {
        log.info("Clearing preference caches");
        preferenceService.refreshPreferenceData();
    }

    /**
     * Clear report caches
     */
    public void clearReportCaches() {
        log.info("Clearing report caches");
        reportService.refreshReportData();
        reliabilityService.refreshReliabilityData(); // Reports affect reliability
    }

    /**
     * Clear user caches
     */
    public void clearUserCaches() {
        log.info("Clearing user caches");
        anonymousUserService.refreshUserData();
        preferenceService.refreshPreferenceData(); // User preferences are affected
    }

    /**
     * Clear caches after data ingestion
     */
    public void clearCachesAfterDataIngestion() {
        log.info("Clearing caches after data ingestion");
        stationService.refreshStationData();
        connectorService.refreshConnectorData();
        reliabilityService.refreshReliabilityData();
    }

    /**
     * Clear anomaly-related caches
     */
    public void clearAnomalyCaches() {
        log.info("Clearing anomaly caches");
        clearCaches("anomalies");
    }

    /**
     * Clear visualization caches
     */
    public void clearVisualizationCaches() {
        log.info("Clearing visualization caches");
        clearCaches("visualizations");
    }
}
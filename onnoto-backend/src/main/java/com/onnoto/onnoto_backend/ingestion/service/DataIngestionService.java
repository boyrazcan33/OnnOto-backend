package com.onnoto.onnoto_backend.ingestion.service;

import com.onnoto.onnoto_backend.ingestion.provider.DataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataIngestionService {

    private final List<DataProvider> dataProviders;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * Fetch all stations from all providers.
     */
    public void fetchAllStations() {
        log.info("Starting full station data fetch from all providers");

        for (DataProvider provider : dataProviders) {
            if (provider.isAvailable()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        log.info("Fetching stations from provider: {}", provider.getProviderName());
                        provider.fetchAllStations();
                        log.info("Completed station fetch from provider: {}", provider.getProviderName());
                    } catch (Exception e) {
                        log.error("Error fetching stations from provider {}: {}",
                                provider.getProviderName(), e.getMessage(), e);
                    }
                }, executorService);
            } else {
                log.warn("Provider {} is not available, skipping station fetch", provider.getProviderName());
            }
        }
    }

    /**
     * Fetch status updates from all providers.
     */
    public void fetchStatusUpdates() {
        log.info("Starting status updates from all providers");

        for (DataProvider provider : dataProviders) {
            if (provider.isAvailable()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        log.info("Fetching status updates from provider: {}", provider.getProviderName());
                        provider.fetchStatusUpdates();
                        log.info("Completed status updates from provider: {}", provider.getProviderName());
                    } catch (Exception e) {
                        log.error("Error fetching status updates from provider {}: {}",
                                provider.getProviderName(), e.getMessage(), e);
                    }
                }, executorService);
            } else {
                log.warn("Provider {} is not available, skipping status updates", provider.getProviderName());
            }
        }
    }
}
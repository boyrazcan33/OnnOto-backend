package com.onnoto.onnoto_backend.ingestion.service;

import com.onnoto.onnoto_backend.ingestion.provider.DataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataIngestionService {

    private final List<DataProvider> dataProviders;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * Fetch all stations from all providers with improved error handling
     */
    public void fetchAllStations() {
        log.info("Starting full station data fetch from all providers");

        ConcurrentHashMap<String, String> errorsByProvider = new ConcurrentHashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);

        for (DataProvider provider : dataProviders) {
            if (provider.isAvailable()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        log.info("Fetching stations from provider: {}", provider.getProviderName());
                        provider.fetchAllStations();
                        successCount.incrementAndGet();
                        log.info("Completed station fetch from provider: {}", provider.getProviderName());
                    } catch (Exception e) {
                        log.error("Error fetching stations from provider {}: {}",
                                provider.getProviderName(), e.getMessage(), e);
                        errorsByProvider.put(provider.getProviderName(), e.getMessage());
                    }
                }, executorService);
            } else {
                log.warn("Provider {} is not available, skipping station fetch", provider.getProviderName());
                errorsByProvider.put(provider.getProviderName(), "Provider not available");
            }
        }

        // Log summary after a delay to allow async tasks to complete
        CompletableFuture.runAsync(() -> {
            try {
                // Give time for tasks to complete
                Thread.sleep(10000);

                // Log summary
                log.info("Station fetch summary - Success: {}, Failures: {}",
                        successCount.get(), errorsByProvider.size());

                if (!errorsByProvider.isEmpty()) {
                    errorsByProvider.forEach((provider, error) ->
                            log.error("Provider {} failed: {}", provider, error));
                }
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for completion", e);
                Thread.currentThread().interrupt();
            }
        }, executorService);
    }

    /**
     * Fetch status updates from all providers with improved error handling
     */
    public void fetchStatusUpdates() {
        log.info("Starting status updates from all providers");

        ConcurrentHashMap<String, String> errorsByProvider = new ConcurrentHashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);

        for (DataProvider provider : dataProviders) {
            if (provider.isAvailable()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        log.info("Fetching status updates from provider: {}", provider.getProviderName());
                        provider.fetchStatusUpdates();
                        successCount.incrementAndGet();
                        log.info("Completed status updates from provider: {}", provider.getProviderName());
                    } catch (Exception e) {
                        log.error("Error fetching status updates from provider {}: {}",
                                provider.getProviderName(), e.getMessage(), e);
                        errorsByProvider.put(provider.getProviderName(), e.getMessage());
                    }
                }, executorService);
            } else {
                log.warn("Provider {} is not available, skipping status updates", provider.getProviderName());
                errorsByProvider.put(provider.getProviderName(), "Provider not available");
            }
        }

        // Log summary after a delay to allow async tasks to complete
        CompletableFuture.runAsync(() -> {
            try {
                // Give time for tasks to complete
                Thread.sleep(10000);

                // Log summary
                log.info("Status updates summary - Success: {}, Failures: {}",
                        successCount.get(), errorsByProvider.size());

                if (!errorsByProvider.isEmpty()) {
                    errorsByProvider.forEach((provider, error) ->
                            log.error("Provider {} failed: {}", provider, error));
                }
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for completion", e);
                Thread.currentThread().interrupt();
            }
        }, executorService);
    }
}
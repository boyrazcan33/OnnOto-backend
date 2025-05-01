package com.onnoto.onnoto_backend.ingestion.service;

import com.onnoto.onnoto_backend.exception.BadRequestException;
import com.onnoto.onnoto_backend.exception.DataProcessingException;
import com.onnoto.onnoto_backend.exception.ResourceNotFoundException;
import com.onnoto.onnoto_backend.ingestion.provider.DataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    private final CacheService cacheService;

    /**
     * Fetch all stations from all providers with improved error handling
     */
    public void fetchAllStations() {
        log.info("Starting full station data fetch from all providers");

        ConcurrentHashMap<String, String> errorsByProvider = new ConcurrentHashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (DataProvider provider : dataProviders) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                String providerName = provider.getProviderName();

                try {
                    if (!provider.isAvailable()) {
                        log.warn("Provider {} is not available, skipping station fetch", providerName);
                        errorsByProvider.put(providerName, "Provider not available");
                        return;
                    }

                    log.info("Fetching stations from provider: {}", providerName);
                    fetchStationsWithRetry(provider);
                    successCount.incrementAndGet();
                    log.info("Completed station fetch from provider: {}", providerName);
                } catch (Exception e) {
                    String errorMessage = String.format("Error fetching stations: %s", e.getMessage());
                    log.error("Error fetching stations from provider {}: {}", providerName, e.getMessage(), e);
                    errorsByProvider.put(providerName, errorMessage);
                }
            }, executorService);

            futures.add(future);
        }

        // Wait for all futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Refresh caches regardless of partial failures
        if (successCount.get() > 0) {
            try {
                cacheService.clearCachesAfterDataIngestion();
            } catch (Exception e) {
                log.error("Error clearing caches after data ingestion: {}", e.getMessage(), e);
            }
        }

        // Log summary of operation
        log.info("Station fetch completed. Success: {}, Failures: {}",
                successCount.get(), errorsByProvider.size());

        if (!errorsByProvider.isEmpty()) {
            errorsByProvider.forEach((provider, error) ->
                    log.error("Provider {} failed: {}", provider, error));

            // If all providers failed, throw exception
            if (successCount.get() == 0) {
                throw new DataProcessingException("All data providers failed during station fetch");
            }
        }
    }

    /**
     * Fetch status updates from all providers with improved error handling
     */
    public void fetchStatusUpdates() {
        log.info("Starting status updates from all providers");

        ConcurrentHashMap<String, String> errorsByProvider = new ConcurrentHashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (DataProvider provider : dataProviders) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                String providerName = provider.getProviderName();

                try {
                    if (!provider.isAvailable()) {
                        log.warn("Provider {} is not available, skipping status updates", providerName);
                        errorsByProvider.put(providerName, "Provider not available");
                        return;
                    }

                    log.info("Fetching status updates from provider: {}", providerName);
                    fetchStatusUpdatesWithRetry(provider);
                    successCount.incrementAndGet();
                    log.info("Completed status updates from provider: {}", providerName);
                } catch (Exception e) {
                    String errorMessage = String.format("Error fetching status updates: %s", e.getMessage());
                    log.error("Error fetching status updates from provider {}: {}", providerName, e.getMessage(), e);
                    errorsByProvider.put(providerName, errorMessage);
                }
            }, executorService);

            futures.add(future);
        }

        // Wait for all futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Refresh caches regardless of partial failures
        if (successCount.get() > 0) {
            try {
                cacheService.clearStatusCaches();
            } catch (Exception e) {
                log.error("Error clearing status caches: {}", e.getMessage(), e);
            }
        }

        // Log summary of operation
        log.info("Status updates completed. Success: {}, Failures: {}",
                successCount.get(), errorsByProvider.size());

        if (!errorsByProvider.isEmpty()) {
            errorsByProvider.forEach((provider, error) ->
                    log.error("Provider {} failed: {}", provider, error));

            // If all providers failed, throw exception
            if (successCount.get() == 0) {
                throw new DataProcessingException("All data providers failed during status updates");
            }
        }
    }

    /**
     * Fetch stations with retry capability
     */
    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    private void fetchStationsWithRetry(DataProvider provider) {
        provider.fetchAllStations();
    }

    /**
     * Fetch status updates with retry capability
     */
    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    private void fetchStatusUpdatesWithRetry(DataProvider provider) {
        provider.fetchStatusUpdates();
    }

    /**
     * Recovery method when retries are exhausted for fetchStations
     */
    @Recover
    private void recoverFetchStations(Exception e, DataProvider provider) {
        log.error("All retries exhausted for fetchStations on provider {}: {}",
                provider.getProviderName(), e.getMessage(), e);
    }

    /**
     * Recovery method when retries are exhausted for fetchStatusUpdates
     */
    @Recover
    private void recoverFetchStatusUpdates(Exception e, DataProvider provider) {
        log.error("All retries exhausted for fetchStatusUpdates on provider {}: {}",
                provider.getProviderName(), e.getMessage(), e);
    }
}
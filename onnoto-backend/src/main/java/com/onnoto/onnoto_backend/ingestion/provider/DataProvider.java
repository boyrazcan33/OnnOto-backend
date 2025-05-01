package com.onnoto.onnoto_backend.ingestion.provider;

import com.onnoto.onnoto_backend.model.Station;

import java.util.List;

/**
 * Interface for charging station data providers.
 */
public interface DataProvider {

    /**
     * Get the name of this data provider.
     */
    String getProviderName();

    /**
     * Fetch all stations from this provider.
     */
    List<Station> fetchAllStations();

    /**
     * Fetch station status updates from this provider.
     */
    void fetchStatusUpdates();

    /**
     * Check if this provider is currently available.
     */
    boolean isAvailable();
}
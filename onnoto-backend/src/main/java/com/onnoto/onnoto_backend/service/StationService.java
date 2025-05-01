package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.dto.request.NearbyRequest;
import com.onnoto.onnoto_backend.dto.request.StationFilterRequest;
import com.onnoto.onnoto_backend.dto.response.StationResponse;
import com.onnoto.onnoto_backend.model.Connector;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.repository.ConnectorRepository;
import com.onnoto.onnoto_backend.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationService {
    private final StationRepository stationRepository;
    private final ConnectorRepository connectorRepository;
    private final ReliabilityService reliabilityService;

    /**
     * Get all stations with basic information using pagination
     */
    @Cacheable(value = "stations", key = "{#page, #size}")
    @Transactional(readOnly = true)
    public List<StationResponse> getAllStations(int page, int size) {
        log.debug("Fetching stations page {} with size {}", page, size);

        // Create pageable with sorting by reliability score descending
        Pageable pageable = PageRequest.of(page, size, Sort.by("reliabilityScore").descending());

        // Get paged result
        Page<Station> stationsPage = stationRepository.findAll(pageable);

        // Convert to DTO
        return stationsPage.getContent().stream()
                .map(this::convertToStationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all stations without pagination (backwards compatibility)
     */
    @Cacheable(value = "stations")
    @Transactional(readOnly = true)
    public List<StationResponse> getAllStations() {
        log.debug("Fetching all stations from database (with default pagination)");
        // Use a reasonable default page size
        return getAllStations(0, 100);
    }

    /**
     * Filter stations based on criteria with pagination
     */
    @Cacheable(value = "stations", key = "{'filter', #request.hashCode(), #page, #size}")
    @Transactional(readOnly = true)
    public List<StationResponse> filterStations(StationFilterRequest request, int page, int size) {
        log.debug("Filtering stations with criteria: {}, page: {}, size: {}", request, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Station> stationsPage;

        // Apply filters
        if (request.getCity() != null) {
            stationsPage = stationRepository.findByCity(request.getCity(), pageable);
        } else {
            // For other filters, we'd need to implement custom repository methods
            // For now, use findAll and manual filtering
            stationsPage = stationRepository.findAll(pageable);

            // Apply manual filtering - this is not efficient but works with existing code
            List<Station> filteredStations = new ArrayList<>();
            for (Station station : stationsPage.getContent()) {
                boolean include = true;

                // Filter by network IDs
                if (request.getNetworkIds() != null && !request.getNetworkIds().isEmpty()) {
                    if (station.getNetwork() == null ||
                            !request.getNetworkIds().contains(station.getNetwork().getId())) {
                        include = false;
                    }
                }

                // Filter by minimum reliability
                if (include && request.getMinimumReliability() != null) {
                    if (station.getReliabilityScore() == null ||
                            station.getReliabilityScore().compareTo(request.getMinimumReliability()) < 0) {
                        include = false;
                    }
                }

                if (include) {
                    filteredStations.add(station);
                }
            }

            // Convert filtered list to responses
            return filteredStations.stream()
                    .map(this::convertToStationResponse)
                    .collect(Collectors.toList());
        }

        // Convert page to response list
        return stationsPage.getContent().stream()
                .map(this::convertToStationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Filter stations based on criteria (backwards compatibility)
     */
    @Cacheable(value = "stations", key = "'filter-' + #request.hashCode()")
    @Transactional(readOnly = true)
    public List<StationResponse> filterStations(StationFilterRequest request) {
        log.debug("Filtering stations with criteria: {}", request);
        // Use reasonable defaults
        return filterStations(request, 0, 100);
    }

    /**
     * Get stations near a location with pagination
     */
    @Cacheable(value = "nearbyStations", key = "{#request.latitude, #request.longitude, #request.radiusInMeters, #page, #size}")
    @Transactional(readOnly = true)
    public List<StationResponse> getNearbyStations(NearbyRequest request, int page, int size) {
        log.debug("Finding stations near lat: {}, lon: {}, radius: {}m, page: {}, size: {}",
                request.getLatitude(), request.getLongitude(), request.getRadiusInMeters(), page, size);

        Pageable pageable = PageRequest.of(page, size);

        // Get stations - use the existing non-paginated method for compatibility
        List<Station> stations = stationRepository.findNearbyStations(
                request.getLongitude(),
                request.getLatitude(),
                request.getRadiusInMeters()
        );

        // Apply manual pagination
        int start = page * size;
        int end = Math.min(start + size, stations.size());

        if (start >= stations.size()) {
            return new ArrayList<>();
        }

        // Convert sublist to response
        return stations.subList(start, end).stream()
                .map(this::convertToStationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get stations near a location (backwards compatibility)
     */
    @Cacheable(value = "nearbyStations", key = "{#request.latitude, #request.longitude, #request.radiusInMeters}")
    @Transactional(readOnly = true)
    public List<StationResponse> getNearbyStations(NearbyRequest request) {
        log.debug("Finding stations near lat: {}, lon: {}, radius: {}m",
                request.getLatitude(), request.getLongitude(), request.getRadiusInMeters());

        // Get all stations and let the client handle limits
        return getNearbyStations(request, 0, Integer.MAX_VALUE);
    }

    /**
     * Clear station-related caches after data updates
     */
    @CacheEvict(value = {"stations", "stationDetails", "nearbyStations"}, allEntries = true)
    @Transactional
    public void refreshStationData() {
        log.info("Refreshed station data caches");
    }

    // Helper method to convert Station entity to StationResponse DTO
    private StationResponse convertToStationResponse(Station station) {
        StationResponse response = new StationResponse();
        response.setId(station.getId());
        response.setName(station.getName());

        if (station.getNetwork() != null) {
            response.setNetworkName(station.getNetwork().getName());
        }

        if (station.getOperator() != null) {
            response.setOperatorName(station.getOperator().getName());
        }

        response.setLatitude(station.getLatitude());
        response.setLongitude(station.getLongitude());
        response.setAddress(station.getAddress());
        response.setCity(station.getCity());
        response.setPostalCode(station.getPostalCode());
        response.setCountry(station.getCountry());
        response.setReliabilityScore(station.getReliabilityScore());
        response.setLastStatusUpdate(station.getLastStatusUpdate());

        // Get connectors efficiently
        List<Connector> connectors = connectorRepository.findByStation(station);
        response.setTotalConnectors(connectors.size());

        long availableCount = connectors.stream()
                .filter(c -> "AVAILABLE".equals(c.getStatus()))
                .count();
        response.setAvailableConnectors((int) availableCount);

        return response;
    }
}
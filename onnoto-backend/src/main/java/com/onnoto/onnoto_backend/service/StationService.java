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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationService {
    private final StationRepository stationRepository;
    private final ConnectorRepository connectorRepository;

    /**
     * Get all stations with pagination
     */
    @Transactional(readOnly = true)
    public List<StationResponse> getAllStationsPaged(int page, int size) {
        log.debug("Fetching stations page {} with size {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("reliabilityScore").descending());

        return stationRepository.findAll(pageable).getContent().stream()
                .map(this::convertToStationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all stations (original method)
     */
    @Cacheable(value = "stations")
    @Transactional(readOnly = true)
    public List<StationResponse> getAllStations() {
        log.debug("Fetching all stations from database");
        return stationRepository.findAll().stream()
                .map(this::convertToStationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get nearby stations with better query approach
     */
    @Transactional(readOnly = true)
    public List<StationResponse> getNearbyStations(NearbyRequest request) {
        log.debug("Finding stations near lat: {}, lon: {}, radius: {}m",
                request.getLatitude(), request.getLongitude(), request.getRadiusInMeters());

        List<Station> stations = stationRepository.findNearbyStations(
                request.getLongitude(),
                request.getLatitude(),
                request.getRadiusInMeters()
        );

        // Apply limit if specified
        if (request.getLimit() != null && stations.size() > request.getLimit()) {
            stations = stations.subList(0, request.getLimit());
        }

        return stations.stream()
                .map(this::convertToStationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Helper method to convert Station entity to StationResponse DTO
     * with more efficient connector processing
     */
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

        // Improved connector handling - use count instead of fetching all
        try {
            int totalConnectors = connectorRepository.countByStationId(station.getId());
            int availableConnectors = connectorRepository.countByStationIdAndStatus(station.getId(), "AVAILABLE");

            response.setTotalConnectors(totalConnectors);
            response.setAvailableConnectors(availableConnectors);
        } catch (Exception e) {
            // Fallback to original approach if count methods are unavailable
            List<Connector> connectors = connectorRepository.findByStation(station);
            response.setTotalConnectors(connectors.size());

            long availableCount = connectors.stream()
                    .filter(c -> "AVAILABLE".equals(c.getStatus()))
                    .count();
            response.setAvailableConnectors((int) availableCount);
        }

        return response;
    }
}
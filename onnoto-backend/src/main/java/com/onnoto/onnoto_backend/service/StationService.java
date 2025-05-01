package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.dto.request.NearbyRequest;
import com.onnoto.onnoto_backend.dto.request.StationFilterRequest;
import com.onnoto.onnoto_backend.dto.response.PagedResponse;
import com.onnoto.onnoto_backend.dto.response.StationResponse;
import com.onnoto.onnoto_backend.exception.ResourceNotFoundException;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.repository.ConnectorRepository;
import com.onnoto.onnoto_backend.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StationService {
    private final StationRepository stationRepository;
    private final ConnectorRepository connectorRepository;

    /**
     * Get all stations with basic information using pagination
     */
    @Cacheable(value = "stations", key = "{#page, #size}")
    @Transactional(readOnly = true)
    public PagedResponse<StationResponse> getAllStations(int page, int size) {
        try {
            log.debug("Fetching stations page {} with size {}", page, size);

            // Create pageable with sorting by reliability score descending
            Pageable pageable = PageRequest.of(page, size, Sort.by("reliabilityScore").descending());

            // Get paged result
            Page<Station> stationsPage = stationRepository.findAll(pageable);

            // Convert to DTO
            List<StationResponse> content = stationsPage.getContent().stream()
                    .map(this::convertToStationResponse)
                    .collect(Collectors.toList());

            return new PagedResponse<>(
                    content,
                    stationsPage.getNumber(),
                    stationsPage.getSize(),
                    stationsPage.getTotalElements(),
                    stationsPage.getTotalPages(),
                    stationsPage.isLast()
            );
        } catch (Exception e) {
            log.error("Error fetching stations: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Filter stations based on criteria with pagination
     */
    @Cacheable(value = "stations", key = "{'filter', #request.hashCode(), #page, #size}")
    @Transactional(readOnly = true)
    public PagedResponse<StationResponse> filterStations(StationFilterRequest request, int page, int size) {
        try {
            log.debug("Filtering stations with criteria: {}, page: {}, size: {}", request, page, size);

            // Use pagination parameters from function arguments
            Pageable pageable = PageRequest.of(page, size);
            Page<Station> stationsPage;

            // Apply filters - in a real implementation, this would use
            // a more sophisticated query builder or Specification pattern
            if (request.getCity() != null) {
                stationsPage = stationRepository.findByCity(request.getCity(), pageable);
            } else if (request.getNetworkIds() != null && !request.getNetworkIds().isEmpty()) {
                // In a real implementation, you'd need a custom repository method for this
                // For now, we're using findAll as placeholder
                stationsPage = stationRepository.findAll(pageable);
            } else if (request.getMinimumReliability() != null) {
                // This would require a custom repository method in a real implementation
                // For now, we'll use findAll as a placeholder
                stationsPage = stationRepository.findAll(pageable);
            } else {
                stationsPage = stationRepository.findAll(pageable);
            }

            // Convert to DTO
            List<StationResponse> content = stationsPage.getContent().stream()
                    .map(this::convertToStationResponse)
                    .collect(Collectors.toList());

            return new PagedResponse<>(
                    content,
                    stationsPage.getNumber(),
                    stationsPage.getSize(),
                    stationsPage.getTotalElements(),
                    stationsPage.getTotalPages(),
                    stationsPage.isLast()
            );
        } catch (Exception e) {
            log.error("Error filtering stations: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get stations near a location with pagination
     */
    @Cacheable(value = "nearbyStations", key = "{#request.latitude, #request.longitude, #request.radiusInMeters, #page, #size}")
    @Transactional(readOnly = true)
    public PagedResponse<StationResponse> getNearbyStations(NearbyRequest request, int page, int size) {
        try {
            log.debug("Finding stations near lat: {}, lon: {}, radius: {}m, page: {}, size: {}",
                    request.getLatitude(), request.getLongitude(), request.getRadiusInMeters(), page, size);

            // Use pagination parameters from function arguments
            Pageable pageable = PageRequest.of(page, size);

            Page<Station> stationsPage = stationRepository.findNearbyStations(
                    request.getLongitude(),
                    request.getLatitude(),
                    request.getRadiusInMeters(),
                    pageable
            );

            // Convert to DTO
            List<StationResponse> content = stationsPage.getContent().stream()
                    .map(this::convertToStationResponse)
                    .collect(Collectors.toList());

            return new PagedResponse<>(
                    content,
                    stationsPage.getNumber(),
                    stationsPage.getSize(),
                    stationsPage.getTotalElements(),
                    stationsPage.getTotalPages(),
                    stationsPage.isLast()
            );
        } catch (Exception e) {
            log.error("Error finding nearby stations: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Helper method to convert Station entity to StationResponse DTO
    private StationResponse convertToStationResponse(Station station) {
        try {
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

            // More efficient approach to get connector counts - using count queries instead of loading all connectors
            int totalConnectors = connectorRepository.countByStationId(station.getId());
            int availableConnectors = connectorRepository.countByStationIdAndStatus(station.getId(), "AVAILABLE");

            response.setTotalConnectors(totalConnectors);
            response.setAvailableConnectors(availableConnectors);

            return response;
        } catch (Exception e) {
            log.error("Error converting station to response: {}", e.getMessage(), e);
            throw e;
        }
    }
}
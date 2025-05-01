package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.dto.request.NearbyRequest;
import com.onnoto.onnoto_backend.dto.request.StationFilterRequest;
import com.onnoto.onnoto_backend.dto.response.ConnectorResponse;
import com.onnoto.onnoto_backend.dto.response.StationDetailResponse;
import com.onnoto.onnoto_backend.dto.response.StationResponse;
import com.onnoto.onnoto_backend.model.Connector;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.repository.ConnectorRepository;
import com.onnoto.onnoto_backend.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StationService {
    private final StationRepository stationRepository;
    private final ConnectorRepository connectorRepository;
    private final ReliabilityService reliabilityService;

    @Transactional(readOnly = true)
    public List<StationResponse> getAllStations() {
        return stationRepository.findAll().stream()
                .map(this::convertToStationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<StationDetailResponse> getStationById(String id) {
        return stationRepository.findById(id)
                .map(this::convertToStationDetailResponse);
    }

    @Transactional(readOnly = true)
    public List<StationResponse> filterStations(StationFilterRequest request) {
        // This would be implemented with a custom query
        // For now, we'll use a basic implementation
        List<Station> stations = stationRepository.findAll();

        // Apply filters
        if (request.getCity() != null) {
            stations = stations.stream()
                    .filter(s -> request.getCity().equals(s.getCity()))
                    .collect(Collectors.toList());
        }

        // More filtering logic would be added here

        return stations.stream()
                .map(this::convertToStationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StationResponse> getNearbyStations(NearbyRequest request) {
        List<Station> stations = stationRepository.findNearbyStations(
                request.getLongitude(),
                request.getLatitude(),
                request.getRadiusInMeters()
        );

        // Apply additional filters if present
        if (request.getFilters() != null) {
            // Filter logic would be applied here
        }

        return stations.stream()
                .limit(request.getLimit())
                .map(this::convertToStationResponse)
                .collect(Collectors.toList());
    }

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

        // Get connector counts
        List<Connector> connectors = connectorRepository.findByStation(station);
        response.setTotalConnectors(connectors.size());

        long availableCount = connectors.stream()
                .filter(c -> "AVAILABLE".equals(c.getStatus()))
                .count();
        response.setAvailableConnectors((int) availableCount);

        return response;
    }

    private StationDetailResponse convertToStationDetailResponse(Station station) {
        StationDetailResponse response = new StationDetailResponse();
        response.setId(station.getId());
        response.setName(station.getName());

        if (station.getNetwork() != null) {
            response.setNetworkName(station.getNetwork().getName());
            response.setNetworkId(station.getNetwork().getId());
        }

        if (station.getOperator() != null) {
            response.setOperatorName(station.getOperator().getName());
            response.setOperatorId(station.getOperator().getId());
        }

        response.setLatitude(station.getLatitude());
        response.setLongitude(station.getLongitude());
        response.setAddress(station.getAddress());
        response.setCity(station.getCity());
        response.setPostalCode(station.getPostalCode());
        response.setCountry(station.getCountry());
        response.setReliabilityScore(station.getReliabilityScore());
        response.setLastStatusUpdate(station.getLastStatusUpdate());

        // Get connectors
        List<ConnectorResponse> connectorResponses = connectorRepository.findByStation(station).stream()
                .map(this::convertToConnectorResponse)
                .collect(Collectors.toList());
        response.setConnectors(connectorResponses);

        // Get reliability details
        reliabilityService.getStationReliability(station.getId())
                .ifPresent(response::setReliability);

        return response;
    }

    private ConnectorResponse convertToConnectorResponse(Connector connector) {
        ConnectorResponse response = new ConnectorResponse();
        response.setId(connector.getId());
        response.setStationId(connector.getStation().getId());
        response.setStationName(connector.getStation().getName());
        response.setConnectorType(connector.getConnectorType());
        response.setPowerKw(connector.getPowerKw());
        response.setCurrentType(connector.getCurrentType());
        response.setStatus(connector.getStatus());
        response.setLastStatusUpdate(connector.getLastStatusUpdate());
        return response;
    }
}
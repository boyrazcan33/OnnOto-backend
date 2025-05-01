package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.dto.response.ConnectorResponse;
import com.onnoto.onnoto_backend.model.Connector;
import com.onnoto.onnoto_backend.repository.ConnectorRepository;
import com.onnoto.onnoto_backend.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectorService {
    private final ConnectorRepository connectorRepository;
    private final StationRepository stationRepository;

    /**
     * Get all connectors for a station
     */
    @Cacheable(value = "connectors", key = "#stationId")
    @Transactional(readOnly = true)
    public List<ConnectorResponse> getConnectorsByStationId(String stationId) {
        log.debug("Fetching connectors for station: {}", stationId);
        return stationRepository.findById(stationId)
                .map(station -> connectorRepository.findByStation(station).stream()
                        .map(this::convertToConnectorResponse)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    /**
     * Get a specific connector by ID
     */
    @Cacheable(value = "connectors", key = "'id-' + #id")
    @Transactional(readOnly = true)
    public Optional<ConnectorResponse> getConnectorById(Long id) {
        log.debug("Fetching connector with ID: {}", id);
        return connectorRepository.findById(id)
                .map(this::convertToConnectorResponse);
    }

    /**
     * Get connectors by type
     */
    @Cacheable(value = "connectors", key = "'type-' + #connectorType")
    @Transactional(readOnly = true)
    public List<ConnectorResponse> getConnectorsByType(String connectorType) {
        log.debug("Fetching connectors of type: {}", connectorType);
        return connectorRepository.findByConnectorType(connectorType).stream()
                .map(this::convertToConnectorResponse)
                .collect(Collectors.toList());
    }

    /**
     * Clear connector-related caches after data updates
     */
    @CacheEvict(value = "connectors", allEntries = true)
    @Transactional
    public void refreshConnectorData() {
        log.info("Refreshed connector data caches");
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
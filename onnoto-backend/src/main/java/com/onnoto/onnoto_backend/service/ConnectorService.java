package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.dto.response.ConnectorResponse;
import com.onnoto.onnoto_backend.model.Connector;
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
public class ConnectorService {
    private final ConnectorRepository connectorRepository;
    private final StationRepository stationRepository;

    @Transactional(readOnly = true)
    public List<ConnectorResponse> getConnectorsByStationId(String stationId) {
        return stationRepository.findById(stationId)
                .map(station -> connectorRepository.findByStation(station).stream()
                        .map(this::convertToConnectorResponse)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public Optional<ConnectorResponse> getConnectorById(Long id) {
        return connectorRepository.findById(id)
                .map(this::convertToConnectorResponse);
    }

    @Transactional(readOnly = true)
    public List<ConnectorResponse> getConnectorsByType(String connectorType) {
        return connectorRepository.findByConnectorType(connectorType).stream()
                .map(this::convertToConnectorResponse)
                .collect(Collectors.toList());
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
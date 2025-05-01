package com.onnoto.onnoto_backend.controller;

import com.onnoto.onnoto_backend.dto.response.ConnectorResponse;
import com.onnoto.onnoto_backend.service.ConnectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connectors")
@RequiredArgsConstructor
public class ConnectorController {

    private final ConnectorService connectorService;

    @GetMapping("/station/{stationId}")
    public List<ConnectorResponse> getConnectorsByStationId(@PathVariable String stationId) {
        return connectorService.getConnectorsByStationId(stationId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConnectorResponse> getConnectorById(@PathVariable Long id) {
        return connectorService.getConnectorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{connectorType}")
    public List<ConnectorResponse> getConnectorsByType(@PathVariable String connectorType) {
        return connectorService.getConnectorsByType(connectorType);
    }
}
package com.onnoto.onnoto_backend.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ConnectorResponse {
    private Long id;
    private String stationId;
    private String stationName;
    private String connectorType;
    private BigDecimal powerKw;
    private String currentType;
    private String status;
    private LocalDateTime lastStatusUpdate;
}
package com.onnoto.onnoto_backend.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StationDetailResponse {
    private String id;
    private String name;
    private String networkName;
    private String networkId;
    private String operatorName;
    private String operatorId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
    private String city;
    private String postalCode;
    private String country;
    private BigDecimal reliabilityScore;
    private List<ConnectorResponse> connectors;
    private LocalDateTime lastStatusUpdate;
    private ReliabilityResponse reliability;
}
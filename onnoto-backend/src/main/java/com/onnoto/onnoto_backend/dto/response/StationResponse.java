package com.onnoto.onnoto_backend.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StationResponse {
    private String id;
    private String name;
    private String networkName;
    private String operatorName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
    private String city;
    private String postalCode;
    private String country;
    private BigDecimal reliabilityScore;
    private int availableConnectors;
    private int totalConnectors;
    private LocalDateTime lastStatusUpdate;
}
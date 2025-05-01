package com.onnoto.onnoto_backend.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReliabilityResponse {
    private String stationId;
    private BigDecimal uptimePercentage;
    private Integer reportCount;
    private BigDecimal averageReportSeverity;
    private LocalDateTime lastDowntime;
    private BigDecimal downtimeFrequency;
    private Integer sampleSize;
}
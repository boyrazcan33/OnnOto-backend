package com.onnoto.onnoto_backend.dto.response;

import com.onnoto.onnoto_backend.model.Anomaly;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AnomalyResponse {
    private Long id;
    private String stationId;
    private String stationName;
    private String anomalyType;
    private String description;
    private String severity;
    private BigDecimal severityScore;
    private boolean resolved;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime lastChecked;
}
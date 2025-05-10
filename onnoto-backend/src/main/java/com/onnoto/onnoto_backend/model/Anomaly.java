package com.onnoto.onnoto_backend.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "anomalies")
public class Anomaly {

    public enum AnomalyType {
        STATUS_FLAPPING,       // Rapid status changes
        EXTENDED_DOWNTIME,     // Unusually long offline periods
        CONNECTOR_MISMATCH,    // Reported connector issues
        PATTERN_DEVIATION,     // Unusual usage patterns
        REPORT_SPIKE           // Sudden increase in user reports
    }

    public enum AnomalySeverity {
        LOW,      // Informational, no immediate action needed
        MEDIUM,   // Concerning, should be monitored
        HIGH      // Critical, requires immediate attention
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_type", nullable = false)
    private AnomalyType anomalyType;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnomalySeverity severity;

    @Column(name = "severity_score")
    private BigDecimal severityScore;

    @Column(name = "is_resolved")
    private Boolean isResolved = false;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "last_checked")
    private LocalDateTime lastChecked;

    // Additional data stored as JSON
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode metadata;
}
package com.onnoto.onnoto_backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reliability_metrics")
public class ReliabilityMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "uptime_percentage")
    private BigDecimal uptimePercentage;

    @Column(name = "report_count")
    private Integer reportCount = 0;

    @Column(name = "average_report_severity")
    private BigDecimal averageReportSeverity;

    @Column(name = "last_downtime")
    private LocalDateTime lastDowntime;

    @Column(name = "downtime_frequency")
    private BigDecimal downtimeFrequency;

    @Column(name = "sample_size")
    private Integer sampleSize;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
package com.onnoto.onnoto_backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reports")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @ManyToOne
    @JoinColumn(name = "device_id", nullable = false)
    private AnonymousUser user;

    @Column(name = "report_type", nullable = false)
    private String reportType;

    private String description;

    private String status = "pending";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
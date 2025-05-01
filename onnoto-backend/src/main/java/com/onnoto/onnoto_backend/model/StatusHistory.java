package com.onnoto.onnoto_backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "status_history")
public class StatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "station_id")
    private Station station;

    @ManyToOne
    @JoinColumn(name = "connector_id")
    private Connector connector;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String source;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}
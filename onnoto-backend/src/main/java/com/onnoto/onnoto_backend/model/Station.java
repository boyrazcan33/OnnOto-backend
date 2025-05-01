package com.onnoto.onnoto_backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "stations")
public class Station {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "operator_id")
    private Operator operator;

    @ManyToOne
    @JoinColumn(name = "network_id")
    private Network network;

    @Column(nullable = false)
    private BigDecimal latitude;

    @Column(nullable = false)
    private BigDecimal longitude;

    private String address;

    private String city;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(length = 2)
    private String country = "EE";

    @Column(name = "last_status_update")
    private LocalDateTime lastStatusUpdate;

    @Column(name = "reliability_score")
    private BigDecimal reliabilityScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "station")
    private List<Connector> connectors;
}
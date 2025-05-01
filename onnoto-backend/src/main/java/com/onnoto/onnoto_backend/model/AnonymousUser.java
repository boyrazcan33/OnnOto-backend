package com.onnoto.onnoto_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;  // This import is correct
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity  // This annotation was missing
@Table(name = "anonymous_users")
public class AnonymousUser {
    @Id
    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "first_seen", nullable = false, updatable = false)
    private LocalDateTime firstSeen;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    @Column(name = "language_preference")
    private String languagePreference = "et";
}
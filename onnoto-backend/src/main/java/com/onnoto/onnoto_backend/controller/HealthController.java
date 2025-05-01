package com.onnoto.onnoto_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        Map<String, Object> status = Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().toString(),
                "version", "1.0.0"
        );
        return ResponseEntity.ok(status);
    }
}
package com.onnoto.onnoto_backend.controller;

import com.onnoto.onnoto_backend.dto.request.PreferenceRequest;
import com.onnoto.onnoto_backend.service.PreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class PreferenceController {

    private final PreferenceService preferenceService;

    @PostMapping
    public ResponseEntity<Void> savePreference(@RequestBody PreferenceRequest request) {
        boolean success = preferenceService.savePreference(request);
        return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<Map<String, String>> getUserPreferences(@PathVariable String deviceId) {
        Map<String, String> preferences = preferenceService.getUserPreferences(deviceId);
        return ResponseEntity.ok(preferences);
    }

    @GetMapping("/{deviceId}/{key}")
    public ResponseEntity<String> getUserPreference(@PathVariable String deviceId, @PathVariable String key) {
        return preferenceService.getUserPreference(deviceId, key)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
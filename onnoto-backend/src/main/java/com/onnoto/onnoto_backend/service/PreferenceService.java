package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.dto.request.PreferenceRequest;
import com.onnoto.onnoto_backend.model.AnonymousUser;
import com.onnoto.onnoto_backend.model.UserPreference;
import com.onnoto.onnoto_backend.repository.AnonymousUserRepository;
import com.onnoto.onnoto_backend.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreferenceService {
    private final UserPreferenceRepository preferenceRepository;
    private final AnonymousUserRepository anonymousUserRepository;

    /**
     * Save a user preference
     */
    @CacheEvict(value = "preferences", key = "#request.deviceId")
    @Transactional
    public boolean savePreference(PreferenceRequest request) {
        log.debug("Saving preference for device: {}, key: {}",
                request.getDeviceId(), request.getPreferenceKey());

        Optional<AnonymousUser> user = anonymousUserRepository.findById(request.getDeviceId());

        if (user.isEmpty()) {
            return false;
        }

        Optional<UserPreference> existingPreference = preferenceRepository.findByUserAndPreferenceKey(
                user.get(), request.getPreferenceKey());

        if (existingPreference.isPresent()) {
            UserPreference preference = existingPreference.get();
            preference.setPreferenceValue(request.getPreferenceValue());
            preference.setUpdatedAt(LocalDateTime.now());
            preferenceRepository.save(preference);
        } else {
            UserPreference newPreference = new UserPreference();
            newPreference.setUser(user.get());
            newPreference.setPreferenceKey(request.getPreferenceKey());
            newPreference.setPreferenceValue(request.getPreferenceValue());
            newPreference.setCreatedAt(LocalDateTime.now());
            newPreference.setUpdatedAt(LocalDateTime.now());
            preferenceRepository.save(newPreference);
        }

        return true;
    }

    /**
     * Get all preferences for a user
     */
    @Cacheable(value = "preferences", key = "#deviceId")
    @Transactional(readOnly = true)
    public Map<String, String> getUserPreferences(String deviceId) {
        log.debug("Getting all preferences for device: {}", deviceId);

        Optional<AnonymousUser> user = anonymousUserRepository.findById(deviceId);

        if (user.isEmpty()) {
            return Map.of();
        }

        Map<String, String> preferences = new HashMap<>();
        preferenceRepository.findByUser(user.get()).forEach(pref ->
                preferences.put(pref.getPreferenceKey(), pref.getPreferenceValue()));

        return preferences;
    }

    /**
     * Get a specific preference for a user
     */
    @Cacheable(value = "preferences", key = "{#deviceId, #key}")
    @Transactional(readOnly = true)
    public Optional<String> getUserPreference(String deviceId, String key) {
        log.debug("Getting preference for device: {}, key: {}", deviceId, key);

        Optional<AnonymousUser> user = anonymousUserRepository.findById(deviceId);

        if (user.isEmpty()) {
            return Optional.empty();
        }

        return preferenceRepository.findByUserAndPreferenceKey(user.get(), key)
                .map(UserPreference::getPreferenceValue);
    }

    /**
     * Clear preference-related caches
     */
    @CacheEvict(value = "preferences", allEntries = true)
    @Transactional
    public void refreshPreferenceData() {
        log.info("Refreshed preference data caches");
    }
}
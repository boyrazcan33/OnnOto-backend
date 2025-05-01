package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.dto.request.PreferenceRequest;
import com.onnoto.onnoto_backend.model.AnonymousUser;
import com.onnoto.onnoto_backend.model.UserPreference;
import com.onnoto.onnoto_backend.repository.AnonymousUserRepository;
import com.onnoto.onnoto_backend.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PreferenceService {
    private final UserPreferenceRepository preferenceRepository;
    private final AnonymousUserRepository anonymousUserRepository;

    @Transactional
    public boolean savePreference(PreferenceRequest request) {
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

    @Transactional(readOnly = true)
    public Map<String, String> getUserPreferences(String deviceId) {
        Optional<AnonymousUser> user = anonymousUserRepository.findById(deviceId);

        if (user.isEmpty()) {
            return Map.of();
        }

        Map<String, String> preferences = new HashMap<>();
        preferenceRepository.findByUser(user.get()).forEach(pref ->
                preferences.put(pref.getPreferenceKey(), pref.getPreferenceValue()));

        return preferences;
    }

    @Transactional(readOnly = true)
    public Optional<String> getUserPreference(String deviceId, String key) {
        Optional<AnonymousUser> user = anonymousUserRepository.findById(deviceId);

        if (user.isEmpty()) {
            return Optional.empty();
        }

        return preferenceRepository.findByUserAndPreferenceKey(user.get(), key)
                .map(UserPreference::getPreferenceValue);
    }
}
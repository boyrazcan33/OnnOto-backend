package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.model.AnonymousUser;
import com.onnoto.onnoto_backend.repository.AnonymousUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnonymousUserService {
    private final AnonymousUserRepository anonymousUserRepository;

    /**
     * Register a new user or update an existing one
     */
    @CacheEvict(value = "users", key = "#deviceId")
    @Transactional
    public String registerOrUpdateUser(String deviceId, String languagePreference) {
        log.debug("Registering/updating user with device ID: {}", deviceId);

        Optional<AnonymousUser> existingUser = anonymousUserRepository.findById(deviceId);

        if (existingUser.isPresent()) {
            AnonymousUser user = existingUser.get();
            user.setLastSeen(LocalDateTime.now());

            if (languagePreference != null) {
                user.setLanguagePreference(languagePreference);
            }

            anonymousUserRepository.save(user);
            log.debug("Updated existing user with device ID: {}", deviceId);
            return deviceId;
        } else {
            // New user
            AnonymousUser newUser = new AnonymousUser();

            // If no device ID provided, generate one
            if (deviceId == null || deviceId.isEmpty()) {
                deviceId = UUID.randomUUID().toString();
            }

            newUser.setDeviceId(deviceId);
            newUser.setFirstSeen(LocalDateTime.now());
            newUser.setLastSeen(LocalDateTime.now());
            newUser.setLanguagePreference(languagePreference != null ? languagePreference : "et");

            anonymousUserRepository.save(newUser);
            log.info("Created new anonymous user with device ID: {}", deviceId);
            return deviceId;
        }
    }

    /**
     * Get user's language preference
     */
    @Cacheable(value = "users", key = "'lang-' + #deviceId")
    @Transactional(readOnly = true)
    public Optional<String> getUserLanguagePreference(String deviceId) {
        log.debug("Getting language preference for device ID: {}", deviceId);
        return anonymousUserRepository.findById(deviceId)
                .map(AnonymousUser::getLanguagePreference);
    }

    /**
     * Clear user-related caches
     */
    @CacheEvict(value = "users", allEntries = true)
    @Transactional
    public void refreshUserData() {
        log.info("Refreshed user data caches");
    }
}
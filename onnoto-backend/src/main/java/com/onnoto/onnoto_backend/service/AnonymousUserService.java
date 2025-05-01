package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.model.AnonymousUser;
import com.onnoto.onnoto_backend.repository.AnonymousUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnonymousUserService {
    private final AnonymousUserRepository anonymousUserRepository;

    @Transactional
    public String registerOrUpdateUser(String deviceId, String languagePreference) {
        Optional<AnonymousUser> existingUser = anonymousUserRepository.findById(deviceId);

        if (existingUser.isPresent()) {
            AnonymousUser user = existingUser.get();
            user.setLastSeen(LocalDateTime.now());

            if (languagePreference != null) {
                user.setLanguagePreference(languagePreference);
            }

            anonymousUserRepository.save(user);
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
            return deviceId;
        }
    }

    @Transactional(readOnly = true)
    public Optional<String> getUserLanguagePreference(String deviceId) {
        return anonymousUserRepository.findById(deviceId)
                .map(AnonymousUser::getLanguagePreference);
    }
}
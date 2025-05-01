package com.onnoto.onnoto_backend.config;

import com.onnoto.onnoto_backend.model.AnonymousUser;
import com.onnoto.onnoto_backend.repository.AnonymousUserRepository;
import com.onnoto.onnoto_backend.service.AnonymousUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceIdAuthFilter extends OncePerRequestFilter {

    private final AnonymousUserService anonymousUserService;
    private final AnonymousUserRepository anonymousUserRepository;
    private static final String DEVICE_ID_HEADER = "X-Device-ID";
    private static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";
    private static final long MAX_SESSION_AGE_MS = 86400000; // 24 hours

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Extract device ID from header
            String deviceId = request.getHeader(DEVICE_ID_HEADER);
            String acceptLanguage = request.getHeader(ACCEPT_LANGUAGE_HEADER);
            String languagePreference = null;

            // Simple language detection from Accept-Language header
            if (acceptLanguage != null) {
                if (acceptLanguage.contains("et")) {
                    languagePreference = "et";
                } else if (acceptLanguage.contains("ru")) {
                    languagePreference = "ru";
                } else if (acceptLanguage.contains("en")) {
                    languagePreference = "en";
                }
            }

            // If no device ID present, generate one
            if (deviceId == null || deviceId.isEmpty()) {
                deviceId = UUID.randomUUID().toString();
                log.debug("Generated new device ID: {}", deviceId);
            }

            final String finalDeviceId = deviceId;

            // Check if user exists and session is valid
            Optional<AnonymousUser> existingUser = anonymousUserRepository.findById(deviceId);

            if (existingUser.isPresent()) {
                AnonymousUser user = existingUser.get();

                // Check if session has expired
                long lastSeenMs = user.getLastSeen().toEpochSecond() * 1000;
                long currentTimeMs = System.currentTimeMillis();

                if (currentTimeMs - lastSeenMs > MAX_SESSION_AGE_MS) {
                    // Session expired, generate new device ID
                    deviceId = UUID.randomUUID().toString();
                    log.debug("Session expired for device ID: {}, generated new ID: {}", finalDeviceId, deviceId);
                }
            }

            // Register or update the user
            final String updatedDeviceId = anonymousUserService.registerOrUpdateUser(deviceId, languagePreference);

            // Set authentication in Security Context
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    updatedDeviceId,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Add the device ID to the response header for the client to store
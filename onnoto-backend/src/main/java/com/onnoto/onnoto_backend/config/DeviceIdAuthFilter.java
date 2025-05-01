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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private static final long SESSION_EXPIRY_HOURS = 24; // 24 hours

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

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

        // Check if session is expired
        boolean sessionExpired = false;
        Optional<AnonymousUser> existingUser = anonymousUserRepository.findById(deviceId);

        if (existingUser.isPresent()) {
            AnonymousUser user = existingUser.get();
            LocalDateTime lastSeen = user.getLastSeen();
            LocalDateTime now = LocalDateTime.now();

            long hoursSinceLastSeen = ChronoUnit.HOURS.between(lastSeen, now);
            if (hoursSinceLastSeen > SESSION_EXPIRY_HOURS) {
                log.debug("Session expired for device ID: {}", deviceId);
                sessionExpired = true;
                // Generate new device ID for expired session
                deviceId = UUID.randomUUID().toString();
            }
        }

        // Register or update the user
        final String updatedDeviceId = anonymousUserService.registerOrUpdateUser(deviceId, languagePreference);

        // Set authentication in Spring Security context if not already set
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    updatedDeviceId,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Set authentication for device ID: {}", updatedDeviceId);
        }

        // Add the device ID to response header
        response.setHeader(DEVICE_ID_HEADER, updatedDeviceId);

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
}
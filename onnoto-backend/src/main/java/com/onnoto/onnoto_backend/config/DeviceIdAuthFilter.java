package com.onnoto.onnoto_backend.config;

import com.onnoto.onnoto_backend.service.AnonymousUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeviceIdAuthFilter extends OncePerRequestFilter {

    private final AnonymousUserService anonymousUserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Extract device ID from header
        String deviceId = request.getHeader("X-Device-ID");
        String acceptLanguage = request.getHeader("Accept-Language");
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
        }

        // Register or update the user
        final String updatedDeviceId = anonymousUserService.registerOrUpdateUser(deviceId, languagePreference);

        // Add the device ID to the response header for the client to store
        response.setHeader("X-Device-ID", updatedDeviceId);

        filterChain.doFilter(request, response);
    }
}
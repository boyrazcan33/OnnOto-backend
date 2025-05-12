package com.onnoto.onnoto_backend.config;

import com.onnoto.onnoto_backend.model.AnonymousUser;
import com.onnoto.onnoto_backend.repository.AnonymousUserRepository;
import com.onnoto.onnoto_backend.service.AnonymousUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class SecurityConfigTest {

    private AnonymousUserService mockUserService;
    private AnonymousUserRepository mockUserRepository;
    private DeviceIdAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    private static final String DEVICE_ID_HEADER = "X-Device-ID";
    private static final String FIXED_DEVICE_ID = "facd33c2-3f1d-44a7-a34b-c3937b891b88";

    @BeforeEach
    void setUp() {
        // Clear security context before each test
        SecurityContextHolder.clearContext();

        // Create mocks
        mockUserService = Mockito.mock(AnonymousUserService.class);
        mockUserRepository = Mockito.mock(AnonymousUserRepository.class);

        // Create filter with mocks
        filter = new DeviceIdAuthFilter(mockUserService, mockUserRepository);

        // Set up mock HTTP objects
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @Test
    public void publicEndpointsShouldBeAccessibleWithoutAuthentication() throws ServletException, IOException {
        // Setup - configure a public endpoint
        request.setRequestURI("/api/health");
        request.setMethod("GET");

        // Setup mock behavior
        when(mockUserService.registerOrUpdateUser(any(), any())).thenReturn(FIXED_DEVICE_ID);

        // Create mock user
        AnonymousUser user = new AnonymousUser();
        user.setDeviceId(FIXED_DEVICE_ID);
        user.setFirstSeen(LocalDateTime.now());
        user.setLastSeen(LocalDateTime.now());
        user.setIsBlocked(false);

        when(mockUserRepository.findById(any())).thenReturn(Optional.of(user));

        // Execute the filter
        filter.doFilterInternal(request, response, filterChain);

        // Verify the filter chain was continued (request was allowed)
        assertTrue(((MockFilterChain) filterChain).getRequest() != null);

        // Verify response has a device ID header
        assertNotNull(response.getHeader(DEVICE_ID_HEADER));
    }

    @Test
    public void missingDeviceIdShouldGenerateNewOne() throws ServletException, IOException {
        // Setup - configure a public endpoint
        request.setRequestURI("/api/health");
        request.setMethod("GET");

        // Setup mock behavior
        when(mockUserService.registerOrUpdateUser(any(), any())).thenReturn(FIXED_DEVICE_ID);

        // Create mock user
        AnonymousUser user = new AnonymousUser();
        user.setDeviceId(FIXED_DEVICE_ID);
        user.setFirstSeen(LocalDateTime.now());
        user.setLastSeen(LocalDateTime.now());
        user.setIsBlocked(false);

        when(mockUserRepository.findById(any())).thenReturn(Optional.of(user));

        // Execute the filter
        filter.doFilterInternal(request, response, filterChain);

        // Verify a device ID was generated
        assertEquals(FIXED_DEVICE_ID, response.getHeader(DEVICE_ID_HEADER));

        // Verify user service was called to register the device
        verify(mockUserService).registerOrUpdateUser(any(), any());
    }

    @Test
    public void validDeviceIdShouldAllowAccess() throws ServletException, IOException {
        // Setup - configure a protected endpoint
        request.setRequestURI("/api/preferences");
        request.setMethod("GET");
        request.addHeader(DEVICE_ID_HEADER, FIXED_DEVICE_ID);

        // Setup mock behavior
        when(mockUserService.registerOrUpdateUser(eq(FIXED_DEVICE_ID), any())).thenReturn(FIXED_DEVICE_ID);

        // Create mock user
        AnonymousUser user = new AnonymousUser();
        user.setDeviceId(FIXED_DEVICE_ID);
        user.setFirstSeen(LocalDateTime.now());
        user.setLastSeen(LocalDateTime.now());
        user.setIsBlocked(false);

        when(mockUserRepository.findById(FIXED_DEVICE_ID)).thenReturn(Optional.of(user));

        // Execute the filter
        filter.doFilterInternal(request, response, filterChain);

        // Verify the filter chain was continued (request was allowed)
        assertTrue(((MockFilterChain) filterChain).getRequest() != null);

        // Verify user service was called with the provided device ID
        ArgumentCaptor<String> deviceIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockUserService).registerOrUpdateUser(deviceIdCaptor.capture(), any());
        assertEquals(FIXED_DEVICE_ID, deviceIdCaptor.getValue());
    }
}
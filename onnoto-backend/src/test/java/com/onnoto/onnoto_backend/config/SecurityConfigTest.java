package com.onnoto.onnoto_backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.onnoto.onnoto_backend.model.AnonymousUser;
import com.onnoto.onnoto_backend.repository.AnonymousUserRepository;
import com.onnoto.onnoto_backend.service.AnonymousUserService;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith({SpringExtension.class, MockitoExtension.class})
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceIdAuthFilter deviceIdAuthFilter;

    @Mock
    private AnonymousUserService mockAnonymousUserService;

    @Mock
    private AnonymousUserRepository mockAnonymousUserRepository;

    @BeforeEach
    void setup() {
        // Inject our mocks into the filter directly
        ReflectionTestUtils.setField(deviceIdAuthFilter, "anonymousUserService", mockAnonymousUserService);
        ReflectionTestUtils.setField(deviceIdAuthFilter, "anonymousUserRepository", mockAnonymousUserRepository);

        // Set up default responses with lenient to avoid UnnecessaryStubbingException
        lenient().when(mockAnonymousUserService.registerOrUpdateUser(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0, String.class) != null
                        ? invocation.getArgument(0) : UUID.randomUUID().toString());
    }

    @Test
    public void publicEndpointsShouldBeAccessibleWithoutAuthentication() throws Exception {
        // Test GET requests to public endpoints
        mockMvc.perform(get("/api/stations"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/connectors/station/elmo_001"))
                .andExpect(status().isOk());
    }

    @Test
    public void protectedEndpointsShouldRequireDeviceId() throws Exception {
        // No mocking needed - default behavior should reject without device ID

        // Test POST requests to protected endpoints without device ID
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stationId\":\"elmo_001\",\"reportType\":\"ISSUE\",\"description\":\"Test\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/stations/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"city\":\"Tallinn\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void validDeviceIdShouldAllowAccessToProtectedEndpoints() throws Exception {
        // Setup mock behavior for anonymous user
        String validDeviceId = UUID.randomUUID().toString();
        AnonymousUser user = new AnonymousUser();
        user.setDeviceId(validDeviceId);
        user.setFirstSeen(LocalDateTime.now());
        user.setLastSeen(LocalDateTime.now());
        user.setIsBlocked(false);

        when(mockAnonymousUserRepository.findById(validDeviceId)).thenReturn(Optional.of(user));
        when(mockAnonymousUserService.registerOrUpdateUser(validDeviceId, null)).thenReturn(validDeviceId);

        // Test with valid device ID
        mockMvc.perform(post("/api/reports")
                        .header("X-Device-ID", validDeviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stationId\":\"elmo_001\",\"reportType\":\"ISSUE\",\"description\":\"Test\",\"deviceId\":\"" + validDeviceId + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    public void blockedDeviceIdShouldBeRejected() throws Exception {
        // Setup mock behavior for blocked user
        String blockedDeviceId = UUID.randomUUID().toString();
        AnonymousUser blockedUser = new AnonymousUser();
        blockedUser.setDeviceId(blockedDeviceId);
        blockedUser.setFirstSeen(LocalDateTime.now());
        blockedUser.setLastSeen(LocalDateTime.now());
        blockedUser.setIsBlocked(true);

        when(mockAnonymousUserRepository.findById(blockedDeviceId)).thenReturn(Optional.of(blockedUser));

        // Configure a new device ID to be issued
        String newDeviceId = UUID.randomUUID().toString();
        when(mockAnonymousUserService.registerOrUpdateUser(anyString(), anyString())).thenReturn(newDeviceId);

        // Test with blocked device ID
        mockMvc.perform(post("/api/reports")
                        .header("X-Device-ID", blockedDeviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stationId\":\"elmo_001\",\"reportType\":\"ISSUE\",\"description\":\"Test\",\"deviceId\":\"" + blockedDeviceId + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Device-ID"))
                .andExpect(header().string("X-Device-ID", newDeviceId));
    }

    @Test
    public void missingDeviceIdShouldGenerateNewOne() throws Exception {
        // Setup mock behavior to generate new device ID
        String newDeviceId = UUID.randomUUID().toString();
        when(mockAnonymousUserService.registerOrUpdateUser(null, null)).thenReturn(newDeviceId);

        // Test request without device ID
        mockMvc.perform(get("/api/stations"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Device-ID"))
                .andExpect(header().string("X-Device-ID", newDeviceId));
    }

    @Test
    public void adminEndpointsShouldBeDeniedForRegularUsers() throws Exception {
        // Setup mock behavior for regular user
        String regularDeviceId = UUID.randomUUID().toString();
        AnonymousUser user = new AnonymousUser();
        user.setDeviceId(regularDeviceId);
        user.setFirstSeen(LocalDateTime.now());
        user.setLastSeen(LocalDateTime.now());
        user.setIsBlocked(false);

        when(mockAnonymousUserRepository.findById(regularDeviceId)).thenReturn(Optional.of(user));
        when(mockAnonymousUserService.registerOrUpdateUser(regularDeviceId, null)).thenReturn(regularDeviceId);

        // Test admin endpoints
        mockMvc.perform(get("/actuator/info")
                        .header("X-Device-ID", regularDeviceId))
                .andExpect(status().isForbidden());
    }
}
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
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
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
        // Test protected endpoints without device ID
        mockMvc.perform(get("/api/preferences/somedeviceid"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Device-ID"));
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

        lenient().when(mockAnonymousUserRepository.findById(validDeviceId)).thenReturn(Optional.of(user));
        lenient().when(mockAnonymousUserService.registerOrUpdateUser(validDeviceId, null)).thenReturn(validDeviceId);

        // Use a GET endpoint for testing protected endpoints - we'll intercept before controller is called
        MvcResult result = mockMvc.perform(get("/api/preferences/" + validDeviceId)
                        .header("X-Device-ID", validDeviceId))
                .andExpect(status().isNotFound())  // 404 is expected since the preference doesn't exist
                .andExpect(header().exists("X-Device-ID"))
                .andReturn();

        // Verify the device ID is returned unchanged
        String returnedDeviceId = result.getResponse().getHeader("X-Device-ID");
        assertEquals(validDeviceId, returnedDeviceId, "Device ID should be unchanged for valid users");
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

        lenient().when(mockAnonymousUserRepository.findById(blockedDeviceId)).thenReturn(Optional.of(blockedUser));

        // Generate a unique ID for the test
        String newDeviceId = UUID.randomUUID().toString();
        lenient().when(mockAnonymousUserService.registerOrUpdateUser(anyString(), anyString()))
                .thenReturn(newDeviceId);

        // Test with blocked device ID on a protected endpoint
        MvcResult result = mockMvc.perform(get("/api/preferences/" + blockedDeviceId)
                        .header("X-Device-ID", blockedDeviceId))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Device-ID"))
                .andReturn();

        // Verify that the returned device ID is different from the blocked one
        String returnedDeviceId = result.getResponse().getHeader("X-Device-ID");
        assertNotEquals(blockedDeviceId, returnedDeviceId, "New device ID should be different from blocked ID");
    }

    @Test
    public void missingDeviceIdShouldGenerateNewOne() throws Exception {
        // Test request without device ID on public endpoint
        MvcResult result = mockMvc.perform(get("/api/stations"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Device-ID"))
                .andReturn();

        // Verify that a valid device ID was generated
        String generatedDeviceId = result.getResponse().getHeader("X-Device-ID");
        assertNotNull(generatedDeviceId, "Device ID should be generated");

        // Validate that it looks like a UUID
        assertTrue(generatedDeviceId.contains("-") && generatedDeviceId.length() == 36,
                "Generated ID should have UUID format");
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

        lenient().when(mockAnonymousUserRepository.findById(regularDeviceId)).thenReturn(Optional.of(user));
        lenient().when(mockAnonymousUserService.registerOrUpdateUser(regularDeviceId, null)).thenReturn(regularDeviceId);

        // Test admin endpoints
        mockMvc.perform(get("/actuator/info")
                        .header("X-Device-ID", regularDeviceId))
                .andExpect(status().isForbidden());
    }
}
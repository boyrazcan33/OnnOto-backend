package com.onnoto.onnoto_backend.controller;

import com.onnoto.onnoto_backend.dto.request.NearbyRequest;
import com.onnoto.onnoto_backend.dto.request.StationFilterRequest;
import com.onnoto.onnoto_backend.dto.response.StationDetailResponse;
import com.onnoto.onnoto_backend.dto.response.StationResponse;
import com.onnoto.onnoto_backend.service.StationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StationControllerTest {

    @Mock
    private StationService stationService;

    @InjectMocks
    private StationController stationController;

    private StationResponse testStationResponse;
    private StationDetailResponse testStationDetailResponse;

    @BeforeEach
    void setUp() {
        // Create test station response
        testStationResponse = new StationResponse();
        testStationResponse.setId("elmo_001");
        testStationResponse.setName("Test Station");
        testStationResponse.setLatitude(new BigDecimal("59.4372"));
        testStationResponse.setLongitude(new BigDecimal("24.7539"));
        testStationResponse.setCity("Tallinn");

        // Create test station detail response
        testStationDetailResponse = new StationDetailResponse();
        testStationDetailResponse.setId("elmo_001");
        testStationDetailResponse.setName("Test Station");
        testStationDetailResponse.setLatitude(new BigDecimal("59.4372"));
        testStationDetailResponse.setLongitude(new BigDecimal("24.7539"));
        testStationDetailResponse.setCity("Tallinn");
    }

    @Test
    void getAllStationsShouldReturnListOfStations() {
        // Given
        when(stationService.getAllStations()).thenReturn(Collections.singletonList(testStationResponse));

        // When
        List<StationResponse> result = stationController.getAllStations();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("elmo_001", result.get(0).getId());

        verify(stationService).getAllStations();
    }

    @Test
    void getStationByIdShouldReturnStationWhenExists() {
        // Given
        when(stationService.getStationById("elmo_001")).thenReturn(Optional.of(testStationDetailResponse));

        // When
        ResponseEntity<StationDetailResponse> result = stationController.getStationById("elmo_001");

        // Then
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("elmo_001", result.getBody().getId());

        verify(stationService).getStationById("elmo_001");
    }

    @Test
    void getStationByIdShouldReturnNotFoundWhenDoesNotExist() {
        // Given
        when(stationService.getStationById("nonexistent")).thenReturn(Optional.empty());

        // When
        ResponseEntity<StationDetailResponse> result = stationController.getStationById("nonexistent");

        // Then
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());

        verify(stationService).getStationById("nonexistent");
    }

    @Test
    void filterStationsShouldCallServiceWithRequest() {
        // Given
        StationFilterRequest request = new StationFilterRequest();
        request.setCity("Tallinn");

        when(stationService.filterStations(any(StationFilterRequest.class)))
                .thenReturn(Collections.singletonList(testStationResponse));

        // When
        List<StationResponse> result = stationController.filterStations(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        verify(stationService).filterStations(request);
    }

    @Test
    void getStationsByCityShouldFilterByCity() {
        // Given
        when(stationService.filterStations(any(StationFilterRequest.class)))
                .thenReturn(Collections.singletonList(testStationResponse));

        // When
        List<StationResponse> result = stationController.getStationsByCity("Tallinn");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        verify(stationService).filterStations(any(StationFilterRequest.class));
    }

    @Test
    void getNearbyStationsShouldCallServiceWithRequest() {
        // Given
        NearbyRequest request = new NearbyRequest();
        request.setLatitude(59.4372);
        request.setLongitude(24.7539);

        when(stationService.getNearbyStations(any(NearbyRequest.class)))
                .thenReturn(Collections.singletonList(testStationResponse));

        // When
        List<StationResponse> result = stationController.getNearbyStations(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        verify(stationService).getNearbyStations(request);
    }

    @Test
    void getNearbyStationsGetShouldCreateRequestAndCallService() {
        // Given
        when(stationService.getNearbyStations(any(NearbyRequest.class)))
                .thenReturn(Collections.singletonList(testStationResponse));

        // When
        List<StationResponse> result = stationController.getNearbyStationsGet(59.4372, 24.7539, 5000, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        verify(stationService).getNearbyStations(any(NearbyRequest.class));
    }
}
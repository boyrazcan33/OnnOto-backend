package com.onnoto.onnoto_backend.controller;

import com.onnoto.onnoto_backend.dto.response.AnomalyResponse;
import com.onnoto.onnoto_backend.service.AnomalyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AnomalyControllerTest {

    @Mock
    private AnomalyService anomalyService;

    @InjectMocks
    private AnomalyController anomalyController;

    private AnomalyResponse testAnomaly1;
    private AnomalyResponse testAnomaly2;

    @BeforeEach
    void setUp() {
        // Create test anomaly 1
        testAnomaly1 = new AnomalyResponse();
        testAnomaly1.setId(1L);
        testAnomaly1.setStationId("elmo_001");
        testAnomaly1.setStationName("Test Station 1");
        testAnomaly1.setAnomalyType("STATUS_FLAPPING");
        testAnomaly1.setDescription("Status changing too frequently");
        testAnomaly1.setSeverity("HIGH");
        testAnomaly1.setSeverityScore(BigDecimal.valueOf(0.8));
        testAnomaly1.setResolved(false);
        testAnomaly1.setDetectedAt(LocalDateTime.now().minusDays(1));

        // Create test anomaly 2
        testAnomaly2 = new AnomalyResponse();
        testAnomaly2.setId(2L);
        testAnomaly2.setStationId("elmo_002");
        testAnomaly2.setStationName("Test Station 2");
        testAnomaly2.setAnomalyType("EXTENDED_DOWNTIME");
        testAnomaly2.setDescription("Station offline for extended period");
        testAnomaly2.setSeverity("MEDIUM");
        testAnomaly2.setSeverityScore(BigDecimal.valueOf(0.5));
        testAnomaly2.setResolved(true);
        testAnomaly2.setDetectedAt(LocalDateTime.now().minusDays(3));
        testAnomaly2.setResolvedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    void getAllAnomaliesShouldReturnAllAnomaliesWhenNoParams() {
        // Given
        when(anomalyService.getAllAnomalies()).thenReturn(Arrays.asList(testAnomaly1, testAnomaly2));

        // When
        List<AnomalyResponse> result = anomalyController.getAllAnomalies(null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(anomalyService).getAllAnomalies();
    }

    @Test
    void getAllAnomaliesShouldReturnUnresolvedAnomaliesWhenParamIsTrue() {
        // Given
        when(anomalyService.getUnresolvedAnomalies()).thenReturn(Collections.singletonList(testAnomaly1));

        // When
        List<AnomalyResponse> result = anomalyController.getAllAnomalies(true, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("STATUS_FLAPPING", result.get(0).getAnomalyType());
        verify(anomalyService).getUnresolvedAnomalies();
    }

    @Test
    void getAllAnomaliesShouldFilterBySeverity() {
        // Given
        when(anomalyService.getAnomaliesBySeverity("HIGH")).thenReturn(Collections.singletonList(testAnomaly1));

        // When
        List<AnomalyResponse> result = anomalyController.getAllAnomalies(null, "HIGH", null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("HIGH", result.get(0).getSeverity());
        verify(anomalyService).getAnomaliesBySeverity("HIGH");
    }

    @Test
    void getAllAnomaliesShouldFilterByType() {
        // Given
        when(anomalyService.getAnomaliesByType("EXTENDED_DOWNTIME")).thenReturn(Collections.singletonList(testAnomaly2));

        // When
        List<AnomalyResponse> result = anomalyController.getAllAnomalies(null, null, "EXTENDED_DOWNTIME");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("EXTENDED_DOWNTIME", result.get(0).getAnomalyType());
        verify(anomalyService).getAnomaliesByType("EXTENDED_DOWNTIME");
    }

    @Test
    void getAnomalyByIdShouldReturnAnomalyWhenExists() {
        // Given
        when(anomalyService.getAnomalyById(1L)).thenReturn(Optional.of(testAnomaly1));

        // When
        ResponseEntity<AnomalyResponse> result = anomalyController.getAnomalyById(1L);

        // Then
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1L, result.getBody().getId());
        verify(anomalyService).getAnomalyById(1L);
    }

    @Test
    void getAnomalyByIdShouldReturnNotFoundWhenDoesNotExist() {
        // Given
        when(anomalyService.getAnomalyById(999L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<AnomalyResponse> result = anomalyController.getAnomalyById(999L);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        verify(anomalyService).getAnomalyById(999L);
    }

    @Test
    void getAnomaliesForStationShouldReturnAnomalies() {
        // Given
        when(anomalyService.getAnomaliesForStation("elmo_001")).thenReturn(Collections.singletonList(testAnomaly1));

        // When
        List<AnomalyResponse> result = anomalyController.getAnomaliesForStation("elmo_001", null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("elmo_001", result.get(0).getStationId());
        verify(anomalyService).getAnomaliesForStation("elmo_001");
    }

    @Test
    void getAnomaliesForStationShouldReturnUnresolvedAnomalies() {
        // Given
        when(anomalyService.getUnresolvedAnomaliesForStation("elmo_001")).thenReturn(Collections.singletonList(testAnomaly1));

        // When
        List<AnomalyResponse> result = anomalyController.getAnomaliesForStation("elmo_001", true);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("elmo_001", result.get(0).getStationId());
        assertFalse(result.get(0).isResolved());
        verify(anomalyService).getUnresolvedAnomaliesForStation("elmo_001");
    }

    @Test
    void getRecentAnomaliesShouldReturnAnomaliesWithinTimeframe() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        when(anomalyService.getAnomaliesSince(any(LocalDateTime.class))).thenReturn(Collections.singletonList(testAnomaly1));

        // When
        List<AnomalyResponse> result = anomalyController.getRecentAnomalies(24);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(anomalyService).getAnomaliesSince(any(LocalDateTime.class));
    }

    @Test
    void getStationsWithMostAnomaliesShouldReturnLimitedResults() {
        // Given
        Object station1Stats = Map.of(
                "stationId", "elmo_001",
                "stationName", "Test Station 1",
                "anomalyCount", 5L
        );

        Object station2Stats = Map.of(
                "stationId", "elmo_002",
                "stationName", "Test Station 2",
                "anomalyCount", 3L
        );

        when(anomalyService.getStationsWithMostAnomalies(2)).thenReturn(Arrays.asList(station1Stats, station2Stats));

        // When
        List<Object> result = anomalyController.getStationsWithMostAnomalies(2);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(anomalyService).getStationsWithMostAnomalies(2);
    }
}
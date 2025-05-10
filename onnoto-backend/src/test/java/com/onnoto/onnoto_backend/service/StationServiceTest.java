package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.dto.request.NearbyRequest;
import com.onnoto.onnoto_backend.dto.request.StationFilterRequest;
import com.onnoto.onnoto_backend.dto.response.StationDetailResponse;
import com.onnoto.onnoto_backend.dto.response.StationResponse;
import com.onnoto.onnoto_backend.model.Connector;
import com.onnoto.onnoto_backend.model.Network;
import com.onnoto.onnoto_backend.model.Operator;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.repository.ConnectorRepository;
import com.onnoto.onnoto_backend.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StationServiceTest {

    @Mock
    private StationRepository stationRepository;

    @Mock
    private ConnectorRepository connectorRepository;

    @Mock
    private ReliabilityService reliabilityService;

    @InjectMocks
    private StationService stationService;

    private Station testStation;
    private Connector testConnector;

    @BeforeEach
    void setUp() {
        // Create test network
        Network network = new Network();
        network.setId("elmo");
        network.setName("ELMO Charging Network");

        // Create test operator
        Operator operator = new Operator();
        operator.setId("enefit");
        operator.setName("Enefit");

        // Create test station
        testStation = new Station();
        testStation.setId("elmo_001");
        testStation.setName("Test Station");
        testStation.setNetwork(network);
        testStation.setOperator(operator);
        testStation.setLatitude(new BigDecimal("59.4372"));
        testStation.setLongitude(new BigDecimal("24.7539"));
        testStation.setAddress("Test Address");
        testStation.setCity("Tallinn");
        testStation.setReliabilityScore(new BigDecimal("85.5"));
        testStation.setLastStatusUpdate(LocalDateTime.now());

        // Create test connector
        testConnector = new Connector();
        testConnector.setId(1L);
        testConnector.setStation(testStation);
        testConnector.setConnectorType("CCS");
        testConnector.setPowerKw(new BigDecimal("50.0"));
        testConnector.setStatus("AVAILABLE");
    }

    @Test
    void getAllStationsShouldReturnMappedDTOs() {
        // Given
        when(stationRepository.findAll()).thenReturn(Collections.singletonList(testStation));
        when(connectorRepository.findByStation(any(Station.class))).thenReturn(Collections.singletonList(testConnector));

        // When
        List<StationResponse> result = stationService.getAllStations();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        StationResponse response = result.get(0);
        assertEquals("elmo_001", response.getId());
        assertEquals("Test Station", response.getName());
        assertEquals("ELMO Charging Network", response.getNetworkName());
        assertEquals("Enefit", response.getOperatorName());
        assertEquals(new BigDecimal("59.4372"), response.getLatitude());
        assertEquals(new BigDecimal("24.7539"), response.getLongitude());
        assertEquals("Tallinn", response.getCity());
        assertEquals(1, response.getTotalConnectors());
        assertEquals(1, response.getAvailableConnectors());

        verify(stationRepository).findAll();
        verify(connectorRepository).findByStation(testStation);
    }

    @Test
    void getStationByIdShouldReturnDetailedResponseWhenStationExists() {
        // Given
        when(stationRepository.findById("elmo_001")).thenReturn(Optional.of(testStation));
        when(connectorRepository.findByStation(testStation)).thenReturn(Collections.singletonList(testConnector));
        when(reliabilityService.getStationReliability("elmo_001")).thenReturn(Optional.empty());

        // When
        Optional<StationDetailResponse> result = stationService.getStationById("elmo_001");

        // Then
        assertTrue(result.isPresent());

        StationDetailResponse response = result.get();
        assertEquals("elmo_001", response.getId());
        assertEquals("Test Station", response.getName());
        assertEquals("ELMO Charging Network", response.getNetworkName());
        assertEquals("elmo", response.getNetworkId());
        assertEquals("Enefit", response.getOperatorName());
        assertEquals("enefit", response.getOperatorId());
        assertNotNull(response.getConnectors());
        assertEquals(1, response.getConnectors().size());
        assertEquals("CCS", response.getConnectors().get(0).getConnectorType());

        verify(stationRepository).findById("elmo_001");
        verify(connectorRepository).findByStation(testStation);
        verify(reliabilityService).getStationReliability("elmo_001");
    }

    @Test
    void getStationByIdShouldReturnEmptyWhenStationDoesNotExist() {
        // Given
        when(stationRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<StationDetailResponse> result = stationService.getStationById("nonexistent");

        // Then
        assertFalse(result.isPresent());
        verify(stationRepository).findById("nonexistent");
        verifyNoInteractions(connectorRepository, reliabilityService);
    }

    @Test
    void filterStationsShouldApplyCityFilter() {
        // Given
        StationFilterRequest request = new StationFilterRequest();
        request.setCity("Tallinn");

        when(stationRepository.findAll()).thenReturn(Arrays.asList(testStation));
        when(connectorRepository.findByStation(any(Station.class))).thenReturn(Collections.singletonList(testConnector));

        // When
        List<StationResponse> result = stationService.filterStations(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("elmo_001", result.get(0).getId());

        verify(stationRepository).findAll();
    }

    @Test
    void getNearbyStationsShouldCallRepositoryWithCorrectParams() {
        // Given
        NearbyRequest request = new NearbyRequest();
        request.setLatitude(59.4372);
        request.setLongitude(24.7539);
        request.setRadiusInMeters(5000.0);
        request.setLimit(10);

        when(stationRepository.findNearbyStations(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Collections.singletonList(testStation));
        when(connectorRepository.findByStation(any(Station.class))).thenReturn(Collections.singletonList(testConnector));

        // When
        List<StationResponse> result = stationService.getNearbyStations(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        verify(stationRepository).findNearbyStations(24.7539, 59.4372, 5000.0);
        verify(connectorRepository).findByStation(testStation);
    }

    @Test
    void refreshStationDataShouldLogInfo() {
        // When
        stationService.refreshStationData();

        // No real assertion possible here since it's just logging, but we verify it doesn't throw exceptions
    }
}
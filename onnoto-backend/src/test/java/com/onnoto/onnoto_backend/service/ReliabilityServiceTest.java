package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.dto.response.ReliabilityResponse;
import com.onnoto.onnoto_backend.model.ReliabilityMetric;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.repository.ReliabilityMetricRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReliabilityServiceTest {

    @Mock
    private ReliabilityMetricRepository reliabilityMetricRepository;

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private ReliabilityService reliabilityService;

    private Station testStation;
    private ReliabilityMetric testMetric;

    @BeforeEach
    void setUp() {
        // Create test station
        testStation = new Station();
        testStation.setId("elmo_001");
        testStation.setName("Test Station");

        // Create test reliability metric
        testMetric = new ReliabilityMetric();
        testMetric.setId(1L);
        testMetric.setStation(testStation);
        testMetric.setUptimePercentage(new BigDecimal("95.5"));
        testMetric.setReportCount(5);
        testMetric.setAverageReportSeverity(new BigDecimal("2.3"));
        testMetric.setDowntimeFrequency(new BigDecimal("4.2"));
        testMetric.setSampleSize(100);
        testMetric.setLastDowntime(LocalDateTime.now().minusDays(7));
        testMetric.setCreatedAt(LocalDateTime.now().minusDays(30));
        testMetric.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void getStationReliabilityShouldReturnMetricsWhenStationExists() {
        // Given
        when(stationRepository.findById("elmo_001")).thenReturn(Optional.of(testStation));
        when(reliabilityMetricRepository.findByStation(testStation)).thenReturn(Optional.of(testMetric));

        // When
        Optional<ReliabilityResponse> result = reliabilityService.getStationReliability("elmo_001");

        // Then
        assertTrue(result.isPresent());

        ReliabilityResponse response = result.get();
        assertEquals("elmo_001", response.getStationId());
        assertEquals(new BigDecimal("95.5"), response.getUptimePercentage());
        assertEquals(Integer.valueOf(5), response.getReportCount());
        assertEquals(new BigDecimal("2.3"), response.getAverageReportSeverity());
        assertEquals(new BigDecimal("4.2"), response.getDowntimeFrequency());
        assertEquals(Integer.valueOf(100), response.getSampleSize());

        verify(stationRepository).findById("elmo_001");
        verify(reliabilityMetricRepository).findByStation(testStation);
    }

    @Test
    void getStationReliabilityShouldReturnEmptyWhenStationDoesNotExist() {
        // Given
        when(stationRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<ReliabilityResponse> result = reliabilityService.getStationReliability("nonexistent");

        // Then
        assertFalse(result.isPresent());
        verify(stationRepository).findById("nonexistent");
        verifyNoInteractions(reliabilityMetricRepository);
    }

    @Test
    void getMostReliableStationsShouldReturnLimitedOrderedList() {
        // Given
        ReliabilityMetric metric1 = new ReliabilityMetric();
        metric1.setStation(testStation);
        metric1.setUptimePercentage(new BigDecimal("95.5"));

        Station station2 = new Station();
        station2.setId("elmo_002");
        ReliabilityMetric metric2 = new ReliabilityMetric();
        metric2.setStation(station2);
        metric2.setUptimePercentage(new BigDecimal("90.0"));

        when(reliabilityMetricRepository.findAllOrderByUptimePercentageDesc())
                .thenReturn(Arrays.asList(metric1, metric2));

        // When
        List<ReliabilityResponse> result = reliabilityService.getMostReliableStations(1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("elmo_001", result.get(0).getStationId());
        assertEquals(new BigDecimal("95.5"), result.get(0).getUptimePercentage());

        verify(reliabilityMetricRepository).findAllOrderByUptimePercentageDesc();
    }

    @Test
    void getStationsWithMinimumReliabilityShouldFilterByUptime() {
        // Given
        BigDecimal minimumUptime = new BigDecimal("90.0");

        when(reliabilityMetricRepository.findAllWithMinimumUptime(minimumUptime))
                .thenReturn(Arrays.asList(testMetric));

        // When
        List<ReliabilityResponse> result = reliabilityService.getStationsWithMinimumReliability(minimumUptime);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("elmo_001", result.get(0).getStationId());

        verify(reliabilityMetricRepository).findAllWithMinimumUptime(minimumUptime);
    }
}
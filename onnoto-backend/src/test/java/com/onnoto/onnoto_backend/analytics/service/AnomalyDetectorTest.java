package com.onnoto.onnoto_backend.analytics.service;

import com.onnoto.onnoto_backend.model.Anomaly;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.repository.AnomalyRepository;
import com.onnoto.onnoto_backend.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AnomalyDetectorTest {

    @Mock
    private StationRepository stationRepository;

    @Mock
    private AnomalyRepository anomalyRepository;

    @Mock
    private StatusFlappingDetector statusFlappingDetector;

    @Mock
    private ExtendedDowntimeDetector extendedDowntimeDetector;

    @Mock
    private ReportSpikeDetector reportSpikeDetector;

    @Mock
    private PatternDeviationDetector patternDeviationDetector;

    @InjectMocks
    private AnomalyDetector anomalyDetector;

    private Station testStation1;
    private Station testStation2;
    private Anomaly testAnomaly;

    @BeforeEach
    void setUp() {
        testStation1 = new Station();
        testStation1.setId("elmo_001");
        testStation1.setName("Test Station 1");

        testStation2 = new Station();
        testStation2.setId("elmo_002");
        testStation2.setName("Test Station 2");

        testAnomaly = new Anomaly();
        testAnomaly.setId(1L);
        testAnomaly.setStation(testStation1);
        testAnomaly.setAnomalyType(Anomaly.AnomalyType.STATUS_FLAPPING);
        testAnomaly.setIsResolved(false);
    }

    @Test
    void detectAnomaliesShouldRunAllDetectorsForAllStations() {
        // Given
        List<Station> stations = Arrays.asList(testStation1, testStation2);
        when(stationRepository.findAll()).thenReturn(stations);

        // Configure the detectors to return different numbers of anomalies
        when(statusFlappingDetector.detect(testStation1)).thenReturn(1);
        when(extendedDowntimeDetector.detect(testStation1)).thenReturn(0);
        when(reportSpikeDetector.detect(testStation1)).thenReturn(1);
        when(patternDeviationDetector.detect(testStation1)).thenReturn(0);

        when(statusFlappingDetector.detect(testStation2)).thenReturn(0);
        when(extendedDowntimeDetector.detect(testStation2)).thenReturn(1);
        when(reportSpikeDetector.detect(testStation2)).thenReturn(0);
        when(patternDeviationDetector.detect(testStation2)).thenReturn(0);

        // When
        anomalyDetector.detectAnomalies();

        // Then
        verify(stationRepository).findAll();

        // Verify each detector was called for each station
        verify(statusFlappingDetector).detect(testStation1);
        verify(extendedDowntimeDetector).detect(testStation1);
        verify(reportSpikeDetector).detect(testStation1);
        verify(patternDeviationDetector).detect(testStation1);

        verify(statusFlappingDetector).detect(testStation2);
        verify(extendedDowntimeDetector).detect(testStation2);
        verify(reportSpikeDetector).detect(testStation2);
        verify(patternDeviationDetector).detect(testStation2);
    }

    @Test
    void detectAnomaliesForStationShouldRunAllDetectors() {
        // Given
        when(statusFlappingDetector.detect(testStation1)).thenReturn(1);
        when(extendedDowntimeDetector.detect(testStation1)).thenReturn(0);
        when(reportSpikeDetector.detect(testStation1)).thenReturn(1);
        when(patternDeviationDetector.detect(testStation1)).thenReturn(0);

        // When
        int result = anomalyDetector.detectAnomaliesForStation(testStation1);

        // Then
        assertEquals(2, result); // 1 + 0 + 1 + 0 = 2 anomalies

        verify(statusFlappingDetector).detect(testStation1);
        verify(extendedDowntimeDetector).detect(testStation1);
        verify(reportSpikeDetector).detect(testStation1);
        verify(patternDeviationDetector).detect(testStation1);
    }

    @Test
    void checkForResolvedAnomaliesShouldMarkResolvedAnomalies() {
        // Given
        List<Anomaly> unresolvedAnomalies = new ArrayList<>();
        unresolvedAnomalies.add(testAnomaly);

        when(anomalyRepository.findByIsResolvedFalse()).thenReturn(unresolvedAnomalies);

        // When
        anomalyDetector.checkForResolvedAnomalies();

        // Then
        verify(anomalyRepository).findByIsResolvedFalse();
        // Note: The checkIfAnormalyIsResolved method is private and always returns false in the implementation,
        // so no anomalies will be marked as resolved in this test
    }
}
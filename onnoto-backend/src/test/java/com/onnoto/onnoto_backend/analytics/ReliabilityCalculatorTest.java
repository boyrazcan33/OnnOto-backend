package com.onnoto.onnoto_backend.analytics;

import com.onnoto.onnoto_backend.analytics.service.ReliabilityCalculator;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.repository.ConnectorRepository;
import com.onnoto.onnoto_backend.repository.ReliabilityMetricRepository;
import com.onnoto.onnoto_backend.repository.ReportRepository;
import com.onnoto.onnoto_backend.repository.StatusHistoryRepository;
import com.onnoto.onnoto_backend.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReliabilityCalculatorTest {

    @Mock
    private StationRepository stationRepository;

    @Mock
    private ConnectorRepository connectorRepository;

    @Mock
    private StatusHistoryRepository statusHistoryRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReliabilityMetricRepository reliabilityMetricRepository;

    private ReliabilityCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ReliabilityCalculator(
                stationRepository,
                connectorRepository,
                statusHistoryRepository,
                reportRepository,
                reliabilityMetricRepository);
    }

    @Test
    void shouldCalculateReliabilityForAllStations() {
        // Given
        Station station1 = new Station();
        station1.setId("station1");

        Station station2 = new Station();
        station2.setId("station2");

        when(stationRepository.findAll()).thenReturn(Arrays.asList(station1, station2));

        // Mock status counts for station1
        StatusHistoryRepository.StatusCountDto availableDto1 = createStatusCountDto("AVAILABLE", 80);
        StatusHistoryRepository.StatusCountDto offlineDto1 = createStatusCountDto("OFFLINE", 20);
        when(statusHistoryRepository.countStatusesByStation(
                eq("station1"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(availableDto1, offlineDto1));

        // Mock status counts for station2
        StatusHistoryRepository.StatusCountDto availableDto2 = createStatusCountDto("AVAILABLE", 90);
        StatusHistoryRepository.StatusCountDto offlineDto2 = createStatusCountDto("OFFLINE", 10);
        when(statusHistoryRepository.countStatusesByStation(
                eq("station2"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(availableDto2, offlineDto2));

        // Mock transition counts
        when(statusHistoryRepository.countStatusTransitions(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L);

        // Mock report counts
        when(reportRepository.countByStationIdAndDateRange(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(2L);

        when(reportRepository.countByStation(any(Station.class))).thenReturn(2);

        when(reliabilityMetricRepository.findByStation(any(Station.class)))
                .thenReturn(Optional.empty());

        // When
        calculator.calculateAllStationReliability();

        // Then
        verify(stationRepository).save(any(Station.class));
        verify(reliabilityMetricRepository).save(any());
    }

    private StatusHistoryRepository.StatusCountDto createStatusCountDto(String status, int count) {
        return new StatusHistoryRepository.StatusCountDto() {
            @Override
            public String getStatus() {
                return status;
            }

            @Override
            public int getCount() {
                return count;
            }
        };
    }
}
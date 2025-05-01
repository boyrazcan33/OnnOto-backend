package com.onnoto.onnoto_backend.ingestion.provider;

import com.onnoto.onnoto_backend.model.Connector;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.model.StatusHistory;
import com.onnoto.onnoto_backend.repository.ConnectorRepository;
import com.onnoto.onnoto_backend.repository.StationRepository;
import com.onnoto.onnoto_backend.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseDataProvider implements DataProvider {

    protected final RestTemplate restTemplate;
    protected final StationRepository stationRepository;
    protected final ConnectorRepository connectorRepository;
    protected final StatusHistoryRepository statusHistoryRepository;

    /**
     * Record a status update for a connector.
     */
    protected void recordStatusUpdate(Connector connector, String status, String source) {
        String oldStatus = connector.getStatus();

        // Only record if status changed
        if (!status.equals(oldStatus)) {
            // Update connector status
            connector.setStatus(status);
            connector.setLastStatusUpdate(LocalDateTime.now());
            connectorRepository.save(connector);

            // Record in history
            StatusHistory history = new StatusHistory();
            history.setConnector(connector);
            history.setStation(connector.getStation());
            history.setStatus(status);
            history.setSource(source);
            history.setRecordedAt(LocalDateTime.now());
            statusHistoryRepository.save(history);

            log.info("Status update for connector {} at station {}: {} -> {} (source: {})",
                    connector.getId(), connector.getStation().getId(), oldStatus, status, source);
        }
    }

    /**
     * Update station's last status update timestamp.
     */
    protected void updateStationTimestamp(Station station) {
        station.setLastStatusUpdate(LocalDateTime.now());
        stationRepository.save(station);
    }
}
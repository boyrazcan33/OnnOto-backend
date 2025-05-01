package com.onnoto.onnoto_backend.repository;

import com.onnoto.onnoto_backend.model.Anomaly;
import com.onnoto.onnoto_backend.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnomalyRepository extends JpaRepository<Anomaly, Long> {

    // Find anomalies by station
    List<Anomaly> findByStation(Station station);

    // Find unresolved anomalies
    List<Anomaly> findByIsResolvedFalse();

    // Find anomalies by type
    List<Anomaly> findByAnomalyType(Anomaly.AnomalyType type);

    // Find anomalies by severity
    List<Anomaly> findBySeverity(Anomaly.AnomalySeverity severity);

    // Find unresolved anomalies for a station
    List<Anomaly> findByStationAndIsResolvedFalse(Station station);

    // Find recently detected anomalies
    @Query("SELECT a FROM Anomaly a WHERE a.detectedAt >= :since ORDER BY a.detectedAt DESC")
    List<Anomaly> findRecentAnomalies(@Param("since") LocalDateTime since);

    // Count unresolved anomalies by station
    long countByStationAndIsResolvedFalse(Station station);

    // Find stations with most unresolved anomalies
    @Query("SELECT a.station, COUNT(a) as anomalyCount FROM Anomaly a " +
            "WHERE a.isResolved = false " +
            "GROUP BY a.station ORDER BY anomalyCount DESC")
    List<Object[]> findStationsWithMostAnomalies(org.springframework.data.domain.Pageable pageable);
}
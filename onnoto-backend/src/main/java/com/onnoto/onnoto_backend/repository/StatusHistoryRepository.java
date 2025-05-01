package com.onnoto.onnoto_backend.repository;

import com.onnoto.onnoto_backend.model.Connector;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.model.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findByStation(Station station);

    List<StatusHistory> findByConnector(Connector connector);

    List<StatusHistory> findByStationAndRecordedAtBetween(
            Station station,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Find status history records by connector and time range
     */
    List<StatusHistory> findByConnectorAndRecordedAtBetween(
            Connector connector,
            LocalDateTime startDate,
            LocalDateTime endDate);

    @Query("SELECT sh FROM StatusHistory sh WHERE sh.connector = ?1 ORDER BY sh.recordedAt DESC")
    List<StatusHistory> findLatestByConnector(Connector connector);
    interface StatusCountDto {
        String getStatus();
        int getCount();
    }

    @Query("SELECT sh.status as status, COUNT(sh) as count FROM StatusHistory sh " +
            "WHERE sh.station.id = :stationId AND sh.recordedAt BETWEEN :startDate AND :endDate " +
            "GROUP BY sh.status")
    List<StatusCountDto> countStatusesByStation(
            @Param("stationId") String stationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT COUNT(*) FROM (" +
            "SELECT status, LAG(status) OVER (ORDER BY recorded_at) as prev_status " +
            "FROM status_history " +
            "WHERE station_id = :stationId AND recorded_at BETWEEN :startDate AND :endDate" +
            ") as subquery " +
            "WHERE status != prev_status", nativeQuery = true)
    long countStatusTransitions(
            @Param("stationId") String stationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
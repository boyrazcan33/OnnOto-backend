package com.onnoto.onnoto_backend.repository;

import com.onnoto.onnoto_backend.model.Connector;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.model.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    @Query("SELECT sh FROM StatusHistory sh WHERE sh.connector = ?1 ORDER BY sh.recordedAt DESC")
    List<StatusHistory> findLatestByConnector(Connector connector);
}
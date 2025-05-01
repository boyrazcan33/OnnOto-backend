package com.onnoto.onnoto_backend.repository;

import com.onnoto.onnoto_backend.model.Connector;
import com.onnoto.onnoto_backend.model.Station;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConnectorRepository extends JpaRepository<Connector, Long> {
    List<Connector> findByStation(Station station);

    Page<Connector> findByStation(Station station, Pageable pageable);

    List<Connector> findByConnectorType(String connectorType);

    List<Connector> findByStatus(String status);

    List<Connector> findByStationAndStatus(Station station, String status);

    // Add count methods to avoid fetching all connectors
    @Query("SELECT COUNT(c) FROM Connector c WHERE c.station.id = :stationId")
    int countByStationId(@Param("stationId") String stationId);

    @Query("SELECT COUNT(c) FROM Connector c WHERE c.station.id = :stationId AND c.status = :status")
    int countByStationIdAndStatus(@Param("stationId") String stationId, @Param("status") String status);
}
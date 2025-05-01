package com.onnoto.onnoto_backend.repository;

import com.onnoto.onnoto_backend.model.AnonymousUser;
import com.onnoto.onnoto_backend.model.Report;
import com.onnoto.onnoto_backend.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByStation(Station station);

    List<Report> findByUser(AnonymousUser user);

    List<Report> findByStatus(String status);

    List<Report> findByStationAndCreatedAtBetween(
            Station station,
            LocalDateTime startDate,
            LocalDateTime endDate
    );



    long countByStationAndStatus(Station station, String status);
    @Query("SELECT COUNT(r) FROM Report r WHERE r.station.id = :stationId " +
            "AND r.createdAt BETWEEN :startDate AND :endDate")
    long countByStationIdAndDateRange(
            @Param("stationId") String stationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    int countByStation(Station station);

}
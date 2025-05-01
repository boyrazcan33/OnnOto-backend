package com.onnoto.onnoto_backend.repository;

import com.onnoto.onnoto_backend.model.ReliabilityMetric;
import com.onnoto.onnoto_backend.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReliabilityMetricRepository extends JpaRepository<ReliabilityMetric, Long> {
    Optional<ReliabilityMetric> findByStation(Station station);

    @Query("SELECT rm FROM ReliabilityMetric rm WHERE rm.uptimePercentage >= ?1 ORDER BY rm.uptimePercentage DESC")
    List<ReliabilityMetric> findAllWithMinimumUptime(BigDecimal minimumUptime);

    @Query("SELECT rm FROM ReliabilityMetric rm WHERE rm.sampleSize >= ?1 ORDER BY rm.uptimePercentage DESC")
    List<ReliabilityMetric> findAllWithMinimumSampleSize(Integer minimumSampleSize);

    @Query("SELECT AVG(rm.uptimePercentage) FROM ReliabilityMetric rm WHERE rm.station.network.id = ?1")
    BigDecimal findAverageUptimeByNetworkId(String networkId);

    @Query("SELECT AVG(rm.uptimePercentage) FROM ReliabilityMetric rm WHERE rm.station.city = ?1")
    BigDecimal findAverageUptimeByCity(String city);

    @Query("SELECT rm FROM ReliabilityMetric rm ORDER BY rm.uptimePercentage DESC")
    List<ReliabilityMetric> findAllOrderByUptimePercentageDesc();

    @Query("SELECT rm FROM ReliabilityMetric rm ORDER BY rm.downtimeFrequency ASC")
    List<ReliabilityMetric> findAllOrderByDowntimeFrequencyAsc();
}
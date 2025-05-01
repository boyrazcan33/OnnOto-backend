package com.onnoto.onnoto_backend.repository;

import com.onnoto.onnoto_backend.model.Station;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StationRepository extends JpaRepository<Station, String> {
    // Modified existing methods to support pagination
    Page<Station> findByCity(String city, Pageable pageable);

    Page<Station> findByNetwork_Id(String networkId, Pageable pageable);

    Page<Station> findByOperator_Id(String operatorId, Pageable pageable);

    @Query(value = "SELECT s.* FROM stations s " +
            "WHERE ST_DWithin(ST_SetSRID(ST_MakePoint(s.longitude, s.latitude), 4326)::geography, " +
            "ST_SetSRID(ST_MakePoint(?1, ?2), 4326)::geography, ?3)",
            countQuery = "SELECT COUNT(*) FROM stations s " +
                    "WHERE ST_DWithin(ST_SetSRID(ST_MakePoint(s.longitude, s.latitude), 4326)::geography, " +
                    "ST_SetSRID(ST_MakePoint(?1, ?2), 4326)::geography, ?3)",
            nativeQuery = true)
    Page<Station> findNearbyStations(double longitude, double latitude, double radiusInMeters, Pageable pageable);

    /**
     * Get average reliability scores grouped by network
     */
    @Query("SELECT s.network.id, s.network.name, AVG(s.reliabilityScore), COUNT(s) " +
            "FROM Station s WHERE s.reliabilityScore IS NOT NULL " +
            "GROUP BY s.network.id, s.network.name ORDER BY AVG(s.reliabilityScore) DESC")
    List<Object[]> getAverageReliabilityByNetwork();
}
package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.dto.request.NearbyRequest;
import com.onnoto.onnoto_backend.dto.response.StationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeospatialService {
    private final StationService stationService;

    /**
     * Calculate distance between two points
     */
    public double calculateDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double lonDistance = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // convert to kilometers
    }

    /**
     * Get stations within a specified radius
     */
    @Cacheable(value = "nearbyStations", key = "{#latitude, #longitude, #radiusInKm}")
    @Transactional(readOnly = true)
    public List<StationResponse> getStationsWithinRadius(double latitude, double longitude, double radiusInKm) {
        log.debug("Finding stations within {}km of coordinates ({}, {})",
                radiusInKm, latitude, longitude);

        // Convert km to meters for the repository method
        double radiusInMeters = radiusInKm * 1000;

        NearbyRequest request = new NearbyRequest();
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setRadiusInMeters(radiusInMeters);

        return stationService.getNearbyStations(request);
    }
}
package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.dto.response.StationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GeospatialService {
    private final StationService stationService;

    // Calculate distance between two points
    public double calculateDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double lonDistance = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c; // convert to kilometers

        return distance;
    }

    // Get stations within a specified radius
    public List<StationResponse> getStationsWithinRadius(double latitude, double longitude, double radiusInKm) {
        // Convert km to meters for the repository method
        double radiusInMeters = radiusInKm * 1000;

        return stationService.getNearbyStations(createNearbyRequest(latitude, longitude, radiusInMeters));
    }

    private com.onnoto.onnoto_backend.dto.request.NearbyRequest createNearbyRequest(
            double latitude, double longitude, double radiusInMeters) {
        com.onnoto.onnoto_backend.dto.request.NearbyRequest request =
                new com.onnoto.onnoto_backend.dto.request.NearbyRequest();
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setRadiusInMeters(radiusInMeters);
        return request;
    }
}
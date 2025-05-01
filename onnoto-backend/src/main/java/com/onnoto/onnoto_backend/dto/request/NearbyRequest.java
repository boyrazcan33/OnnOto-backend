package com.onnoto.onnoto_backend.dto.request;

import lombok.Data;

@Data
public class NearbyRequest {
    private double latitude;
    private double longitude;
    private double radiusInMeters = 5000.0; // Default 5km
    private Integer limit = 10;
    private StationFilterRequest filters;
}
package com.onnoto.onnoto_backend.dto.request;

import lombok.Data;

@Data
public class ReportRequest {
    private String stationId;
    private String deviceId;
    private String reportType;
    private String description;
}
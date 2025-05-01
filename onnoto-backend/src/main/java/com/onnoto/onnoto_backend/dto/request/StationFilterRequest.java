package com.onnoto.onnoto_backend.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StationFilterRequest {
    private List<String> networkIds;
    private List<String> connectorTypes;
    private List<String> statuses;
    private BigDecimal minimumReliability;
    private String city;
    private Integer limit;
    private Integer offset;
}
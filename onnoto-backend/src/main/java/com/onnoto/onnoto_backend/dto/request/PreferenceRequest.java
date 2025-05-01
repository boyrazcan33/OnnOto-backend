package com.onnoto.onnoto_backend.dto.request;

import lombok.Data;

@Data
public class PreferenceRequest {
    private String deviceId;
    private String preferenceKey;
    private String preferenceValue;
}
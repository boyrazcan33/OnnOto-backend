package com.onnoto.onnoto_backend.controller;

import com.onnoto.onnoto_backend.dto.request.ReportRequest;
import com.onnoto.onnoto_backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<Void> createReport(@RequestBody ReportRequest request) {
        Optional<Long> reportId = reportService.createReport(request);

        if (reportId.isPresent()) {
            URI location = URI.create("/api/reports/" + reportId.get());
            return ResponseEntity.created(location).build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/count/station/{stationId}")
    public ResponseEntity<Long> getReportCountForStation(@PathVariable String stationId) {
        long count = reportService.getReportCountForStation(stationId);
        return ResponseEntity.ok(count);
    }
}
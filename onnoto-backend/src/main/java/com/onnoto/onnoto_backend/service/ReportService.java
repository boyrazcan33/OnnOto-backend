package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.dto.request.ReportRequest;
import com.onnoto.onnoto_backend.model.AnonymousUser;
import com.onnoto.onnoto_backend.model.Report;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.repository.AnonymousUserRepository;
import com.onnoto.onnoto_backend.repository.ReportRepository;
import com.onnoto.onnoto_backend.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final StationRepository stationRepository;
    private final AnonymousUserRepository anonymousUserRepository;

    /**
     * Create a new station report
     */
    @CacheEvict(value = {"stations", "stationDetails", "reliability"}, allEntries = true)
    @Transactional
    public Optional<Long> createReport(ReportRequest request) {
        log.info("Creating report for station: {} from device: {}",
                request.getStationId(), request.getDeviceId());

        Optional<Station> station = stationRepository.findById(request.getStationId());
        Optional<AnonymousUser> user = anonymousUserRepository.findById(request.getDeviceId());

        if (station.isEmpty() || user.isEmpty()) {
            return Optional.empty();
        }

        Report report = new Report();
        report.setStation(station.get());
        report.setUser(user.get());
        report.setReportType(request.getReportType());
        report.setDescription(request.getDescription());
        report.setStatus("pending");
        report.setCreatedAt(LocalDateTime.now());

        Report savedReport = reportRepository.save(report);
        log.info("Saved report with ID: {}", savedReport.getId());

        return Optional.of(savedReport.getId());
    }

    /**
     * Get the report count for a station
     */
    @Cacheable(value = "reports", key = "'count-' + #stationId")
    @Transactional(readOnly = true)
    public long getReportCountForStation(String stationId) {
        log.debug("Getting report count for station: {}", stationId);
        return stationRepository.findById(stationId)
                .map(reportRepository::countByStation)
                .orElse(0);
    }

    /**
     * Clear report-related caches
     */
    @CacheEvict(value = "reports", allEntries = true)
    @Transactional
    public void refreshReportData() {
        log.info("Refreshed report data caches");
    }
}
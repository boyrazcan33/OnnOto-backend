package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.dto.request.ReportRequest;
import com.onnoto.onnoto_backend.model.AnonymousUser;
import com.onnoto.onnoto_backend.model.Report;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.repository.AnonymousUserRepository;
import com.onnoto.onnoto_backend.repository.ReportRepository;
import com.onnoto.onnoto_backend.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final StationRepository stationRepository;
    private final AnonymousUserRepository anonymousUserRepository;

    @Transactional
    public Optional<Long> createReport(ReportRequest request) {
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
        return Optional.of(savedReport.getId());
    }

    @Transactional(readOnly = true)
    public long getReportCountForStation(String stationId) {
        return stationRepository.findById(stationId)
                .map(station -> reportRepository.countByStationAndStatus(station, "pending"))
                .orElse(0L);
    }
}
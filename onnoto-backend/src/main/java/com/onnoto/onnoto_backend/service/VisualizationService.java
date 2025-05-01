package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.model.Anomaly;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.model.StatusHistory;
import com.onnoto.onnoto_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisualizationService {

    private final StationRepository stationRepository;
    private final NetworkRepository networkRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final ReliabilityMetricRepository reliabilityMetricRepository;
    private final AnomalyRepository anomalyRepository;

    /**
     * Get reliability score distribution for visualization
     */
    @Cacheable(value = "visualizations", key = "'reliability-distribution'")
    @Transactional(readOnly = true)
    public Map<String, Object> getReliabilityDistribution() {
        log.debug("Generating reliability distribution data");

        List<Station> stations = stationRepository.findAll();

        // Group stations by reliability score ranges
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("90-100", 0);
        distribution.put("80-89", 0);
        distribution.put("70-79", 0);
        distribution.put("60-69", 0);
        distribution.put("50-59", 0);
        distribution.put("0-49", 0);

        for (Station station : stations) {
            BigDecimal score = station.getReliabilityScore();
            if (score == null) {
                continue;
            }

            int scoreInt = score.intValue();

            if (scoreInt >= 90) {
                distribution.put("90-100", distribution.get("90-100") + 1);
            } else if (scoreInt >= 80) {
                distribution.put("80-89", distribution.get("80-89") + 1);
            } else if (scoreInt >= 70) {
                distribution.put("70-79", distribution.get("70-79") + 1);
            } else if (scoreInt >= 60) {
                distribution.put("60-69", distribution.get("60-69") + 1);
            } else if (scoreInt >= 50) {
                distribution.put("50-59", distribution.get("50-59") + 1);
            } else {
                distribution.put("0-49", distribution.get("0-49") + 1);
            }
        }

        // Format data for charts
        List<Map<String, Object>> chartData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("range", entry.getKey());
            dataPoint.put("count", entry.getValue());
            chartData.add(dataPoint);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("data", chartData);
        result.put("total", stations.size());

        return result;
    }

    /**
     * Get reliability metrics grouped by network
     */
    @Cacheable(value = "visualizations", key = "'reliability-by-network'")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getReliabilityByNetwork() {
        log.debug("Generating reliability by network data");

        List<Object[]> networkData = stationRepository.getAverageReliabilityByNetwork();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] data : networkData) {
            String networkId = (String) data[0];
            String networkName = (String) data[1];
            Double avgReliability = (Double) data[2];
            Long stationCount = (Long) data[3];

            Map<String, Object> networkStats = new HashMap<>();
            networkStats.put("networkId", networkId);
            networkStats.put("networkName", networkName);
            networkStats.put("averageReliability", avgReliability);
            networkStats.put("stationCount", stationCount);

            result.add(networkStats);
        }

        return result;
    }

    /**
     * Get status history for a station over time
     */
    @Cacheable(value = "visualizations", key = "'status-history-' + #stationId + '-' + #startDate + '-' + #endDate")
    @Transactional(readOnly = true)
    public Map<String, Object> getStatusHistory(String stationId, LocalDate startDate, LocalDate endDate) {
        log.debug("Generating status history for station: {} from {} to {}",
                stationId, startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        Optional<Station> stationOpt = stationRepository.findById(stationId);
        if (stationOpt.isEmpty()) {
            return Map.of("error", "Station not found");
        }

        Station station = stationOpt.get();
        List<StatusHistory> history = statusHistoryRepository.findByStationAndRecordedAtBetween(
                station, startDateTime, endDateTime);

        // Group by date and status
        Map<LocalDate, Map<String, Integer>> dailyStatusCounts = new HashMap<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE;

        for (StatusHistory record : history) {
            LocalDate recordDate = record.getRecordedAt().toLocalDate();
            String status = record.getStatus();

            dailyStatusCounts.putIfAbsent(recordDate, new HashMap<>());
            Map<String, Integer> dayCounts = dailyStatusCounts.get(recordDate);

            dayCounts.put(status, dayCounts.getOrDefault(status, 0) + 1);
        }

        // Format for chart display
        List<String> dates = new ArrayList<>();
        List<Map<String, Object>> series = new ArrayList<>();

        // Create status series
        Map<String, List<Integer>> statusSeries = new HashMap<>();
        statusSeries.put("AVAILABLE", new ArrayList<>());
        statusSeries.put("OCCUPIED", new ArrayList<>());
        statusSeries.put("OFFLINE", new ArrayList<>());

        // Fill data for each date
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            dates.add(current.format(dateFormatter));

            Map<String, Integer> dayCounts = dailyStatusCounts.getOrDefault(current, Map.of());

            for (String status : statusSeries.keySet()) {
                statusSeries.get(status).add(dayCounts.getOrDefault(status, 0));
            }

            current = current.plusDays(1);
        }

        // Create series objects
        for (Map.Entry<String, List<Integer>> entry : statusSeries.entrySet()) {
            Map<String, Object> seriesData = new HashMap<>();
            seriesData.put("name", entry.getKey());
            seriesData.put("data", entry.getValue());
            series.add(seriesData);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("station", Map.of(
                "id", station.getId(),
                "name", station.getName(),
                "network", station.getNetwork() != null ? station.getNetwork().getName() : null
        ));
        result.put("dates", dates);
        result.put("series", series);

        return result;
    }

    /**
     * Get anomaly trends over time
     */
    @Cacheable(value = "visualizations", key = "'anomaly-trends-' + #days")
    @Transactional(readOnly = true)
    public Map<String, Object> getAnomalyTrends(int days) {
        log.debug("Generating anomaly trends for the last {} days", days);

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Anomaly> anomalies = anomalyRepository.findRecentAnomalies(startDate);

        // Group by day and type
        Map<LocalDate, Map<Anomaly.AnomalyType, Integer>> dailyTypeCounts = new HashMap<>();

        for (Anomaly anomaly : anomalies) {
            LocalDate recordDate = anomaly.getDetectedAt().toLocalDate();
            Anomaly.AnomalyType type = anomaly.getAnomalyType();

            dailyTypeCounts.putIfAbsent(recordDate, new HashMap<>());
            Map<Anomaly.AnomalyType, Integer> dayCounts = dailyTypeCounts.get(recordDate);

            dayCounts.put(type, dayCounts.getOrDefault(type, 0) + 1);
        }

        // Format for chart display
        List<String> dates = new ArrayList<>();
        List<Map<String, Object>> series = new ArrayList<>();

        // Create type series
        Map<Anomaly.AnomalyType, List<Integer>> typeSeries = new HashMap<>();
        for (Anomaly.AnomalyType type : Anomaly.AnomalyType.values()) {
            typeSeries.put(type, new ArrayList<>());
        }

        // Fill data for each date
        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE;
        LocalDate current = LocalDate.now().minusDays(days);
        LocalDate today = LocalDate.now();

        while (!current.isAfter(today)) {
            dates.add(current.format(dateFormatter));

            Map<Anomaly.AnomalyType, Integer> dayCounts = dailyTypeCounts.getOrDefault(current, Map.of());

            for (Anomaly.AnomalyType type : typeSeries.keySet()) {
                typeSeries.get(type).add(dayCounts.getOrDefault(type, 0));
            }

            current = current.plusDays(1);
        }

        // Create series objects
        for (Map.Entry<Anomaly.AnomalyType, List<Integer>> entry : typeSeries.entrySet()) {
            Map<String, Object> seriesData = new HashMap<>();
            seriesData.put("name", entry.getKey().name());
            seriesData.put("data", entry.getValue());
            series.add(seriesData);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("series", series);

        return result;
    }

    /**
     * Get geographic heatmap data of reliability scores
     */
    @Cacheable(value = "visualizations", key = "'geographic-heatmap'")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getGeographicHeatmap() {
        log.debug("Generating geographic heatmap data");

        List<Station> stations = stationRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Station station : stations) {
            if (station.getLatitude() == null || station.getLongitude() == null) {
                continue;
            }

            Map<String, Object> point = new HashMap<>();
            point.put("id", station.getId());
            point.put("name", station.getName());
            point.put("lat", station.getLatitude());
            point.put("lng", station.getLongitude());

            // For heatmap intensity - use inverse of reliability
            // (higher reliability = lower heat intensity)
            BigDecimal score = station.getReliabilityScore();
            if (score != null) {
                // Score is 0-100, convert to 0-1 range and invert
                // Lower reliability = higher heat intensity
                double intensity = 1.0 - (score.doubleValue() / 100.0);
                point.put("intensity", intensity);
            } else {
                point.put("intensity", 0.5); // Default mid-value
            }

            result.add(point);
        }

        return result;
    }

    /**
     * Get usage patterns for all stations
     */
    @Cacheable(value = "visualizations", key = "'usage-patterns-overall'")
    @Transactional(readOnly = true)
    public Map<String, Object> getOverallUsagePatterns() {
        log.debug("Generating overall usage patterns");

        // This would analyze historical data to detect patterns
        // For now, we'll generate sample data

        // Usage by hour of day (0-23)
        int[] hourlyUsage = new int[24];
        // Usage by day of week (0-6, Sunday is 0)
        int[] dailyUsage = new int[7];

        // Sample data - in a real implementation, this would be calculated from actual data
        hourlyUsage = new int[] {5, 3, 2, 1, 2, 5, 15, 25, 30, 28, 25, 30, 35, 30, 28, 25, 30, 40, 35, 25, 20, 15, 10, 7};
        dailyUsage = new int[] {20, 35, 30, 32, 35, 40, 28};

        Map<String, Object> result = new HashMap<>();

        // Format hourly data
        List<Map<String, Object>> hourlyData = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            Map<String, Object> hour = new HashMap<>();
            hour.put("hour", i);
            hour.put("usage", hourlyUsage[i]);
            hourlyData.add(hour);
        }

        // Format daily data
        List<Map<String, Object>> dailyData = new ArrayList<>();
        String[] dayNames = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        for (int i = 0; i < 7; i++) {
            Map<String, Object> day = new HashMap<>();
            day.put("day", dayNames[i]);
            day.put("usage", dailyUsage[i]);
            dailyData.add(day);
        }

        result.put("hourly", hourlyData);
        result.put("daily", dailyData);

        return result;
    }

    /**
     * Get usage patterns for a specific station
     */
    @Cacheable(value = "visualizations", key = "'usage-patterns-' + #stationId")
    @Transactional(readOnly = true)
    public Map<String, Object> getStationUsagePatterns(String stationId) {
        log.debug("Generating usage patterns for station: {}", stationId);

        // Similar to overall patterns, but specific to one station
        // For now, return similar structure with randomized data

        Optional<Station> stationOpt = stationRepository.findById(stationId);
        if (stationOpt.isEmpty()) {
            return Map.of("error", "Station not found");
        }

        Station station = stationOpt.get();

        // In a real implementation, this would be calculated from historical status data
        // For demonstration, we'll generate random data
        Random random = new Random(stationId.hashCode()); // Use consistent seed for demo

        // Usage by hour of day (0-23)
        int[] hourlyUsage = new int[24];
        // Usage by day of week (0-6, Sunday is 0)
        int[] dailyUsage = new int[7];

        // Generate random usage patterns
        for (int i = 0; i < 24; i++) {
            // Morning and evening peaks
            if (i >= 7 && i <= 9) {
                hourlyUsage[i] = 10 + random.nextInt(20);
            } else if (i >= 16 && i <= 19) {
                hourlyUsage[i] = 15 + random.nextInt(25);
            } else {
                hourlyUsage[i] = 5 + random.nextInt(15);
            }
        }

        for (int i = 0; i < 7; i++) {
            // Weekdays higher than weekends
            if (i >= 1 && i <= 5) {
                dailyUsage[i] = 20 + random.nextInt(30);
            } else {
                dailyUsage[i] = 10 + random.nextInt(20);
            }
        }

        Map<String, Object> result = new HashMap<>();

        // Station info
        result.put("station", Map.of(
                "id", station.getId(),
                "name", station.getName(),
                "network", station.getNetwork() != null ? station.getNetwork().getName() : null
        ));

        // Format hourly data
        List<Map<String, Object>> hourlyData = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            Map<String, Object> hour = new HashMap<>();
            hour.put("hour", i);
            hour.put("usage", hourlyUsage[i]);
            hourlyData.add(hour);
        }

        // Format daily data
        List<Map<String, Object>> dailyData = new ArrayList<>();
        String[] dayNames = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        for (int i = 0; i < 7; i++) {
            Map<String, Object> day = new HashMap<>();
            day.put("day", dayNames[i]);
            day.put("usage", dailyUsage[i]);
            dailyData.add(day);
        }

        result.put("hourly", hourlyData);
        result.put("daily", dailyData);

        return result;
    }
}
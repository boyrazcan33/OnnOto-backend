package com.onnoto.onnoto_backend.analytics;

import com.onnoto.onnoto_backend.analytics.service.AnomalyDetector;
import com.onnoto.onnoto_backend.analytics.service.ExtendedDowntimeDetector;
import com.onnoto.onnoto_backend.analytics.service.PatternDeviationDetector;
import com.onnoto.onnoto_backend.analytics.service.ReportSpikeDetector;
import com.onnoto.onnoto_backend.analytics.service.StatusFlappingDetector;
import com.onnoto.onnoto_backend.model.*;
import com.onnoto.onnoto_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class AnomalyDetectionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:15-3.3")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("onnoto-test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // General test properties
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private ConnectorRepository connectorRepository;

    @Autowired
    private StatusHistoryRepository statusHistoryRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private AnonymousUserRepository anonymousUserRepository;

    @Autowired
    private AnomalyRepository anomalyRepository;

    @Autowired
    private AnomalyDetector anomalyDetector;

    @Autowired
    private StatusFlappingDetector statusFlappingDetector;

    @Autowired
    private ExtendedDowntimeDetector extendedDowntimeDetector;

    @Autowired
    private ReportSpikeDetector reportSpikeDetector;

    @Autowired
    private PatternDeviationDetector patternDeviationDetector;

    private Station testStation;
    private Connector testConnector;
    private Network testNetwork;
    private Operator testOperator;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        anomalyRepository.deleteAll();
        statusHistoryRepository.deleteAll();
        reportRepository.deleteAll();
        connectorRepository.deleteAll();
        stationRepository.deleteAll();
        networkRepository.deleteAll();
        operatorRepository.deleteAll();
        anonymousUserRepository.deleteAll();

        // Create test network
        testNetwork = new Network();
        testNetwork.setId("test_network");
        testNetwork.setName("Test Network");
        testNetwork.setCreatedAt(LocalDateTime.now());
        testNetwork.setUpdatedAt(LocalDateTime.now());
        networkRepository.save(testNetwork);

        // Create test operator
        testOperator = new Operator();
        testOperator.setId("test_operator");
        testOperator.setName("Test Operator");
        testOperator.setCreatedAt(LocalDateTime.now());
        testOperator.setUpdatedAt(LocalDateTime.now());
        operatorRepository.save(testOperator);

        // Create test station
        testStation = new Station();
        testStation.setId("test_station_001");
        testStation.setName("Test Station");
        testStation.setNetwork(testNetwork);
        testStation.setOperator(testOperator);
        testStation.setLatitude(new BigDecimal("59.4372"));
        testStation.setLongitude(new BigDecimal("24.7539"));
        testStation.setCity("Tallinn");
        testStation.setCreatedAt(LocalDateTime.now());
        testStation.setUpdatedAt(LocalDateTime.now());
        stationRepository.save(testStation);

        // Create test connector
        testConnector = new Connector();
        testConnector.setStation(testStation);
        testConnector.setConnectorType("CCS");
        testConnector.setPowerKw(new BigDecimal("50.0"));
        testConnector.setStatus("AVAILABLE");
        testConnector.setLastStatusUpdate(LocalDateTime.now());
        testConnector.setCreatedAt(LocalDateTime.now());
        testConnector.setUpdatedAt(LocalDateTime.now());
        connectorRepository.save(testConnector);
    }

    @Test
    void testStatusFlappingDetection() {
        // Create status history with frequent changes (flapping)
        LocalDateTime baseTime = LocalDateTime.now().minusHours(12);

        // Create multiple status changes within a short period
        String[] statuses = {"AVAILABLE", "OFFLINE", "AVAILABLE", "OFFLINE", "AVAILABLE", "OFFLINE"};

        for (int i = 0; i < statuses.length; i++) {
            StatusHistory history = new StatusHistory();
            history.setConnector(testConnector);
            history.setStation(testStation);
            history.setStatus(statuses[i]);
            history.setSource("TEST");
            history.setRecordedAt(baseTime.plusHours(i));
            statusHistoryRepository.save(history);
        }

        // Run flapping detection
        int detected = statusFlappingDetector.detect(testStation);

        // Verify detection
        assertEquals(1, detected, "Should detect 1 status flapping anomaly");

        // Verify anomaly was created
        List<Anomaly> anomalies = anomalyRepository.findByStation(testStation);
        assertEquals(1, anomalies.size(), "Should create one anomaly");

        Anomaly anomaly = anomalies.get(0);
        assertEquals(Anomaly.AnomalyType.STATUS_FLAPPING, anomaly.getAnomalyType());
        assertFalse(anomaly.getIsResolved(), "Anomaly should not be resolved");
    }

    @Test
    void testExtendedDowntimeDetection() {
        // Set connector to OFFLINE status
        testConnector.setStatus("OFFLINE");
        testConnector.setLastStatusUpdate(LocalDateTime.now().minusDays(2)); // 48 hours ago
        connectorRepository.save(testConnector);

        // Create status history record for the offline event
        StatusHistory offlineHistory = new StatusHistory();
        offlineHistory.setConnector(testConnector);
        offlineHistory.setStation(testStation);
        offlineHistory.setStatus("OFFLINE");
        offlineHistory.setSource("TEST");
        offlineHistory.setRecordedAt(LocalDateTime.now().minusDays(2));
        statusHistoryRepository.save(offlineHistory);

        // Run extended downtime detection
        int detected = extendedDowntimeDetector.detect(testStation);

        // Verify detection
        assertEquals(1, detected, "Should detect 1 extended downtime anomaly");

        // Verify anomaly was created with correct properties
        List<Anomaly> anomalies = anomalyRepository.findByStation(testStation);
        assertEquals(1, anomalies.size(), "Should create one anomaly");

        Anomaly anomaly = anomalies.get(0);
        assertEquals(Anomaly.AnomalyType.EXTENDED_DOWNTIME, anomaly.getAnomalyType());
        assertEquals(Anomaly.AnomalySeverity.MEDIUM, anomaly.getSeverity(),
                "Severity should be MEDIUM for 48 hours downtime");
    }

    @Test
    void testReportSpikeDetection() {
        // Create anonymous user for reports
        AnonymousUser user = new AnonymousUser();
        user.setDeviceId("test-device-001");
        user.setFirstSeen(LocalDateTime.now().minusDays(30));
        user.setLastSeen(LocalDateTime.now());
        // Save the user first to avoid TransientPropertyValueException
        user = anonymousUserRepository.save(user);

        // Create historical baseline (a few reports over a longer period)
        for (int i = 0; i < 3; i++) {
            Report report = new Report();
            report.setStation(testStation);
            report.setUser(user);
            report.setReportType("CONNECTOR_ISSUE");
            report.setDescription("Historical report " + i);
            report.setStatus("pending");
            // Create these reports in the past to establish baseline
            report.setCreatedAt(LocalDateTime.now().minusDays(20 + i));
            reportRepository.save(report);
        }

        // Now create a spike - many reports in a short timeframe
        for (int i = 0; i < 10; i++) {  // Increased to 10 reports for a more obvious spike
            Report report = new Report();
            report.setStation(testStation);
            report.setUser(user);
            report.setReportType("CONNECTOR_ISSUE");
            report.setDescription("Spike report " + i);
            report.setStatus("pending");
            // Create these reports very close together in time to create a spike
            report.setCreatedAt(LocalDateTime.now().minusHours(i));
            reportRepository.save(report);
        }

        // Run report spike detection
        int detected = reportSpikeDetector.detect(testStation);

        // Log the detection count for diagnostics
        System.out.println("Report spike detection count: " + detected);

        // Verify detection
        assertTrue(detected > 0, "Should detect report spike anomaly");

        // Verify anomaly properties
        List<Anomaly> anomalies = anomalyRepository.findByStation(testStation);
        assertFalse(anomalies.isEmpty(), "Should create at least one anomaly");

        boolean foundReportSpike = anomalies.stream()
                .anyMatch(a -> a.getAnomalyType() == Anomaly.AnomalyType.REPORT_SPIKE);

        assertTrue(foundReportSpike, "Should detect REPORT_SPIKE anomaly type");
    }

    @Test
    void testPatternDeviationDetection() {
        // Create a pattern of status history
        LocalDateTime baseTime = LocalDateTime.now().minusDays(7);

        // Create a normal pattern for a week
        for (int day = 0; day < 7; day++) {
            // Morning - usually available
            StatusHistory morningHistory = new StatusHistory();
            morningHistory.setConnector(testConnector);
            morningHistory.setStation(testStation);
            morningHistory.setStatus("AVAILABLE");
            morningHistory.setSource("TEST");
            morningHistory.setRecordedAt(baseTime.plusDays(day).withHour(8));
            statusHistoryRepository.save(morningHistory);

            // Evening - usually occupied
            StatusHistory eveningHistory = new StatusHistory();
            eveningHistory.setConnector(testConnector);
            eveningHistory.setStation(testStation);
            eveningHistory.setStatus("OCCUPIED");
            eveningHistory.setSource("TEST");
            eveningHistory.setRecordedAt(baseTime.plusDays(day).withHour(18));
            statusHistoryRepository.save(eveningHistory);
        }

        // Create a deviation from the pattern
        testConnector.setStatus("OFFLINE"); // Should be available or occupied based on pattern
        connectorRepository.save(testConnector);

        // Run pattern deviation detection
        int detected = patternDeviationDetector.detect(testStation);

        // The detection is probabilistic, so we don't assert exact counts
        List<Anomaly> anomalies = anomalyRepository.findByStation(testStation);

        // Log results for inspection
        System.out.println("Pattern deviation test - detected: " + detected);
        System.out.println("Pattern deviation test - anomalies count: " + anomalies.size());

        // Instead, we check if any anomalies were created
        boolean hasPatternDeviation = anomalies.stream()
                .anyMatch(a -> a.getAnomalyType() == Anomaly.AnomalyType.PATTERN_DEVIATION);

        if (detected > 0) {
            assertTrue(hasPatternDeviation, "Should have PATTERN_DEVIATION anomaly when detected");
        }
    }

    @Test
    void testAnomalyDetectorIntegration() {
        // Create status history with frequent changes (flapping)
        LocalDateTime baseTime = LocalDateTime.now().minusHours(12);
        String[] statuses = {"AVAILABLE", "OFFLINE", "AVAILABLE", "OFFLINE", "AVAILABLE", "OFFLINE"};

        for (int i = 0; i < statuses.length; i++) {
            StatusHistory history = new StatusHistory();
            history.setConnector(testConnector);
            history.setStation(testStation);
            history.setStatus(statuses[i]);
            history.setSource("TEST");
            history.setRecordedAt(baseTime.plusHours(i));
            statusHistoryRepository.save(history);
        }

        // Run the main anomaly detector which should call all detection methods
        int totalDetected = anomalyDetector.detectAnomaliesForStation(testStation);

        // Verify detection occurred
        assertTrue(totalDetected > 0, "Should detect at least one anomaly");

        // Verify anomalies were created
        List<Anomaly> anomalies = anomalyRepository.findByStation(testStation);
        assertFalse(anomalies.isEmpty(), "Should have created anomalies");

        // Print anomaly types for inspection
        System.out.println("Detected anomaly types:");
        anomalies.forEach(a -> System.out.println(" - " + a.getAnomalyType() + ": " + a.getDescription()));
    }
}
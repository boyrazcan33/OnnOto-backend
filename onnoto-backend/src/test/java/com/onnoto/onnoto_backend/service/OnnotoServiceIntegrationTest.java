package com.onnoto.onnoto_backend.service;

import com.onnoto.onnoto_backend.analytics.service.AnomalyDetector;
import com.onnoto.onnoto_backend.analytics.service.ReliabilityCalculator;
import com.onnoto.onnoto_backend.dto.response.StationDetailResponse;
import com.onnoto.onnoto_backend.model.*;
import com.onnoto.onnoto_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
        "spring.data.redis.repositories.enabled=false",
        "spring.cache.type=none"
})
@Testcontainers
@ActiveProfiles("test")
public class OnnotoServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:15-3.3")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("onnoto-test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL properties
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Completely disable Redis and caching
        registry.add("spring.cache.type", () -> "none");
        registry.add("spring.data.redis.repositories.enabled", () -> "false");
        registry.add("management.health.redis.enabled", () -> "false");

        // General test properties
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.task.scheduling.enabled", () -> "false");
        registry.add("app.disable-seeders", () -> "true");
    }

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private ConnectorRepository connectorRepository;

    @Autowired
    private AnomalyRepository anomalyRepository;

    @Autowired
    private ReliabilityMetricRepository reliabilityMetricRepository;

    @Autowired
    private AnomalyDetector anomalyDetector;

    @Autowired
    private ReliabilityCalculator reliabilityCalculator;

    @Autowired
    private StationService stationService;

    @BeforeEach
    void setUp() {
        // Clear all data before each test
        anomalyRepository.deleteAll();
        reliabilityMetricRepository.deleteAll();
        connectorRepository.deleteAll();
        stationRepository.deleteAll();
        networkRepository.deleteAll();
        operatorRepository.deleteAll();
    }

    @Test
    void testDataIngestionToReliabilityCalculationFlow() {
        // 1. Setup initial data (mock the data ingestion process)
        Network network = createNetwork("test_network", "Test Network");
        Operator operator = createOperator("test_operator", "Test Operator");
        Station station = createStation("test_station", "Test Station", network, operator);

        // Create connectors with different statuses
        createConnector(station, "CCS", "AVAILABLE");
        createConnector(station, "Type 2", "OFFLINE");

        // 2. Run reliability calculation
        reliabilityCalculator.calculateStationReliability(station);

        // 3. Verify reliability metrics were created
        Optional<ReliabilityMetric> metrics = reliabilityMetricRepository.findByStation(station);

        assertTrue(metrics.isPresent());
        assertNotNull(metrics.get().getUptimePercentage());

        // 4. Verify station reliability score was updated
        Optional<Station> updatedStation = stationRepository.findById(station.getId());

        assertTrue(updatedStation.isPresent());
        assertNotNull(updatedStation.get().getReliabilityScore());
    }

    @Test
    void testAnomalyDetectionFlow() {
        // 1. Setup initial data
        Network network = createNetwork("test_network", "Test Network");
        Operator operator = createOperator("test_operator", "Test Operator");
        Station station = createStation("test_station", "Test Station", network, operator);

        // Create connector with status history to trigger anomaly detection
        Connector connector = createConnector(station, "CCS", "OFFLINE");

        // 2. Run anomaly detection
        int detected = anomalyDetector.detectAnomaliesForStation(station);

        // 3. Verify anomalies were created (this will depend on your anomaly detection rules)
        List<Anomaly> anomalies = anomalyRepository.findByStation(station);

        // Log output for debugging
        System.out.println("Detected anomalies: " + detected);
        System.out.println("Retrieved anomalies: " + anomalies.size());

        // This assertion may need adjustment based on your detection rules
        assertEquals(detected, anomalies.size());
    }

    @Test
    void testStationServiceIntegrationWithReliabilityData() {
        // 1. Setup initial data
        Network network = createNetwork("test_network", "Test Network");
        Operator operator = createOperator("test_operator", "Test Operator");
        Station station = createStation("test_station", "Test Station", network, operator);

        // Create connectors
        createConnector(station, "CCS", "AVAILABLE");

        // 2. Create reliability data
        ReliabilityMetric metric = new ReliabilityMetric();
        metric.setStation(station);
        metric.setUptimePercentage(new BigDecimal("95.5"));
        metric.setReportCount(5);
        metric.setDowntimeFrequency(new BigDecimal("4.2"));
        metric.setSampleSize(100);
        metric.setCreatedAt(LocalDateTime.now());
        metric.setUpdatedAt(LocalDateTime.now());
        reliabilityMetricRepository.save(metric);

        // Update station with reliability score
        station.setReliabilityScore(new BigDecimal("95.5"));
        stationRepository.save(station);

        // 3. Get station details from service
        Optional<StationDetailResponse> stationDetail = stationService.getStationById(station.getId());

        // 4. Verify station details include reliability data
        assertTrue(stationDetail.isPresent());
        assertNotNull(stationDetail.get().getReliability());
        assertEquals(0, new BigDecimal("95.5").compareTo(stationDetail.get().getReliability().getUptimePercentage()),
                "BigDecimal values should be numerically equal");
    }

    // Helper methods to create test entities
    private Network createNetwork(String id, String name) {
        Network network = new Network();
        network.setId(id);
        network.setName(name);
        network.setCreatedAt(LocalDateTime.now());
        network.setUpdatedAt(LocalDateTime.now());
        return networkRepository.save(network);
    }

    private Operator createOperator(String id, String name) {
        Operator operator = new Operator();
        operator.setId(id);
        operator.setName(name);
        operator.setCreatedAt(LocalDateTime.now());
        operator.setUpdatedAt(LocalDateTime.now());
        return operatorRepository.save(operator);
    }

    private Station createStation(String id, String name, Network network, Operator operator) {
        Station station = new Station();
        station.setId(id);
        station.setName(name);
        station.setNetwork(network);
        station.setOperator(operator);
        station.setLatitude(new BigDecimal("59.4372"));
        station.setLongitude(new BigDecimal("24.7539"));
        station.setCity("Tallinn");
        station.setCreatedAt(LocalDateTime.now());
        station.setUpdatedAt(LocalDateTime.now());
        return stationRepository.save(station);
    }

    private Connector createConnector(Station station, String type, String status) {
        Connector connector = new Connector();
        connector.setStation(station);
        connector.setConnectorType(type);
        connector.setPowerKw(new BigDecimal("50.0"));
        connector.setCurrentType("DC");
        connector.setStatus(status);
        connector.setLastStatusUpdate(LocalDateTime.now());
        connector.setCreatedAt(LocalDateTime.now());
        connector.setUpdatedAt(LocalDateTime.now());
        return connectorRepository.save(connector);
    }
}
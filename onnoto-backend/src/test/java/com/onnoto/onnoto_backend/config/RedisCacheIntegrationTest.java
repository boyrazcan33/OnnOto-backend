package com.onnoto.onnoto_backend.config;

import com.onnoto.onnoto_backend.dto.response.StationDetailResponse;
import com.onnoto.onnoto_backend.dto.response.StationResponse;
import com.onnoto.onnoto_backend.model.Connector;
import com.onnoto.onnoto_backend.model.Network;
import com.onnoto.onnoto_backend.model.Operator;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.repository.ConnectorRepository;
import com.onnoto.onnoto_backend.repository.NetworkRepository;
import com.onnoto.onnoto_backend.repository.OperatorRepository;
import com.onnoto.onnoto_backend.repository.StationRepository;
import com.onnoto.onnoto_backend.service.StationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@EnableCaching
@Import(RedisConfig.class)
public class RedisCacheIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:15-3.3")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("onnoto-test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database properties
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Redis properties
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        registry.add("spring.redis.ssl", () -> false);

        // Other configuration
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StationService stationService;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private ConnectorRepository connectorRepository;

    private Station testStation;

    @BeforeEach
    void setUp() {
        // Clear cache
        cacheManager.getCacheNames().forEach(cacheName ->
                Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());

        // Clear database
        connectorRepository.deleteAll();
        stationRepository.deleteAll();
        networkRepository.deleteAll();
        operatorRepository.deleteAll();

        // Create test data
        Network network = new Network();
        network.setId("test_network");
        network.setName("Test Network");
        network.setCreatedAt(LocalDateTime.now());
        network.setUpdatedAt(LocalDateTime.now());
        networkRepository.save(network);

        Operator operator = new Operator();
        operator.setId("test_operator");
        operator.setName("Test Operator");
        operator.setCreatedAt(LocalDateTime.now());
        operator.setUpdatedAt(LocalDateTime.now());
        operatorRepository.save(operator);

        Station station = new Station();
        station.setId("test_station_001");
        station.setName("Test Station");
        station.setNetwork(network);
        station.setOperator(operator);
        station.setLatitude(new BigDecimal("59.4372"));
        station.setLongitude(new BigDecimal("24.7539"));
        station.setCity("Tallinn");
        station.setCreatedAt(LocalDateTime.now());
        station.setUpdatedAt(LocalDateTime.now());
        testStation = stationRepository.save(station);

        Connector connector = new Connector();
        connector.setStation(testStation);
        connector.setConnectorType("CCS");
        connector.setPowerKw(new BigDecimal("50.0"));
        connector.setStatus("AVAILABLE");
        connector.setCreatedAt(LocalDateTime.now());
        connector.setUpdatedAt(LocalDateTime.now());
        connectorRepository.save(connector);
    }

    @Test
    void testStationsCaching() {
        // First call - should hit the database
        List<StationResponse> firstResult = stationService.getAllStations();

        // Verify first call
        assertNotNull(firstResult);
        assertFalse(firstResult.isEmpty());
        assertEquals("test_station_001", firstResult.get(0).getId());

        // Add a new station that shouldn't be in the cached result
        Station newStation = new Station();
        newStation.setId("test_station_002");
        newStation.setName("Another Test Station");
        newStation.setLatitude(new BigDecimal("59.4372"));
        newStation.setLongitude(new BigDecimal("24.7539"));
        newStation.setCity("Tallinn");
        newStation.setCreatedAt(LocalDateTime.now());
        newStation.setUpdatedAt(LocalDateTime.now());
        stationRepository.save(newStation);

        // Second call - should use cache
        List<StationResponse> secondResult = stationService.getAllStations();

        // Verify second call - still has only one station from cache
        assertNotNull(secondResult);
        assertEquals(1, secondResult.size());
        assertEquals("test_station_001", secondResult.get(0).getId());
    }

    @Test
    void testStationDetailCaching() {
        // First call - cache miss
        Optional<StationDetailResponse> firstResult = stationService.getStationById("test_station_001");

        // Verify first call result
        assertTrue(firstResult.isPresent());
        assertEquals("test_station_001", firstResult.get().getId());

        // Modify station name
        testStation.setName("Updated Station Name");
        stationRepository.save(testStation);

        // Second call - should use cache
        Optional<StationDetailResponse> secondResult = stationService.getStationById("test_station_001");

        // Verify second call - still has old name from cache
        assertTrue(secondResult.isPresent());
        assertEquals("Test Station", secondResult.get().getName());
    }

    @Test
    void testCacheEviction() {
        // First call - should hit the database and cache the result
        List<StationResponse> firstResult = stationService.getAllStations();
        assertNotNull(firstResult);
        assertEquals(1, firstResult.size());
        assertEquals("Test Station", firstResult.get(0).getName());

        // Modify the database
        testStation.setName("Updated Station Name");
        stationRepository.save(testStation);

        // Second call - should use cached data (old name)
        List<StationResponse> cachedResult = stationService.getAllStations();
        assertEquals("Test Station", cachedResult.get(0).getName());

        // Cache eviction
        stationService.refreshStationData();

        // Third call after eviction - should hit the database again
        List<StationResponse> secondResult = stationService.getAllStations();

        // Verify we got updated data (cache was evicted)
        assertEquals("Updated Station Name", secondResult.get(0).getName());
    }
}
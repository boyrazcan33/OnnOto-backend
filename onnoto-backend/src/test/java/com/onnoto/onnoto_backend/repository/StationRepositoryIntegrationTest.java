package com.onnoto.onnoto_backend.repository;

import com.onnoto.onnoto_backend.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class StationRepositoryIntegrationTest {

    // Use PostGIS container
    static DockerImageName postgisImage = DockerImageName.parse("postgis/postgis:15-3.3")
            .asCompatibleSubstituteFor("postgres");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(postgisImage)
            .withDatabaseName("onnoto-test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.spatial.dialect.postgis.PostgisPG10Dialect");
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

    private Network testNetwork;
    private Operator testOperator;
    private Station testStation;

    @BeforeEach
    void setUp() {
        // Create and save test network
        Network network = new Network();
        network.setId("test_network");
        network.setName("Test Network");
        network.setWebsite("https://test.network");
        network.setCreatedAt(LocalDateTime.now());
        network.setUpdatedAt(LocalDateTime.now());
        testNetwork = networkRepository.save(network);

        // Create and save test operator
        Operator operator = new Operator();
        operator.setId("test_operator");
        operator.setName("Test Operator");
        operator.setContactInfo("contact@test.operator");
        operator.setCreatedAt(LocalDateTime.now());
        operator.setUpdatedAt(LocalDateTime.now());
        testOperator = operatorRepository.save(operator);

        // Create and save test station
        Station station = new Station();
        station.setId("test_station_001");
        station.setName("Test Station");
        station.setNetwork(testNetwork);
        station.setOperator(testOperator);
        station.setLatitude(new BigDecimal("59.4372"));
        station.setLongitude(new BigDecimal("24.7539"));
        station.setAddress("Test Address 123");
        station.setCity("Tallinn");
        station.setPostalCode("12345");
        station.setCountry("EE");
        station.setCreatedAt(LocalDateTime.now());
        station.setUpdatedAt(LocalDateTime.now());
        testStation = stationRepository.save(station);
    }

    @Test
    void testStationConnectorRelationship() {
        // Create and save connectors for the test station
        Connector connector1 = new Connector();
        connector1.setStation(testStation);
        connector1.setConnectorType("CCS");
        connector1.setPowerKw(new BigDecimal("50.0"));
        connector1.setCurrentType("DC");
        connector1.setStatus("AVAILABLE");
        connector1.setCreatedAt(LocalDateTime.now());
        connector1.setUpdatedAt(LocalDateTime.now());
        connectorRepository.save(connector1);

        Connector connector2 = new Connector();
        connector2.setStation(testStation);
        connector2.setConnectorType("Type 2");
        connector2.setPowerKw(new BigDecimal("22.0"));
        connector2.setCurrentType("AC");
        connector2.setStatus("AVAILABLE");
        connector2.setCreatedAt(LocalDateTime.now());
        connector2.setUpdatedAt(LocalDateTime.now());
        connectorRepository.save(connector2);

        // Test finding connectors by station
        List<Connector> connectors = connectorRepository.findByStation(testStation);

        // Verify relationship
        assertEquals(2, connectors.size());
        assertTrue(connectors.stream().anyMatch(c -> c.getConnectorType().equals("CCS")));
        assertTrue(connectors.stream().anyMatch(c -> c.getConnectorType().equals("Type 2")));
    }

    @Test
    void testFindStationsByCity() {
        // Create another station in a different city
        Station anotherStation = new Station();
        anotherStation.setId("test_station_002");
        anotherStation.setName("Another Test Station");
        anotherStation.setNetwork(testNetwork);
        anotherStation.setOperator(testOperator);
        anotherStation.setLatitude(new BigDecimal("58.3780"));
        anotherStation.setLongitude(new BigDecimal("26.7290"));
        anotherStation.setAddress("Another Address");
        anotherStation.setCity("Tartu");
        anotherStation.setCountry("EE");
        anotherStation.setCreatedAt(LocalDateTime.now());
        anotherStation.setUpdatedAt(LocalDateTime.now());
        stationRepository.save(anotherStation);

        // Test finding by city
        List<Station> tallinnStations = stationRepository.findByCity("Tallinn");
        List<Station> tartuStations = stationRepository.findByCity("Tartu");

        // Verify city filtering
        assertEquals(1, tallinnStations.size());
        assertEquals("Test Station", tallinnStations.get(0).getName());

        assertEquals(1, tartuStations.size());
        assertEquals("Another Test Station", tartuStations.get(0).getName());
    }

    @Test
    void testFindNearbyStations() {
        // Create another station
        Station farStation = new Station();
        farStation.setId("test_station_003");
        farStation.setName("Far Station");
        farStation.setNetwork(testNetwork);
        farStation.setOperator(testOperator);
        // This is about 165km from Tallinn
        farStation.setLatitude(new BigDecimal("58.3780"));
        farStation.setLongitude(new BigDecimal("26.7290"));
        farStation.setCity("Tartu");
        farStation.setCountry("EE");
        farStation.setCreatedAt(LocalDateTime.now());
        farStation.setUpdatedAt(LocalDateTime.now());
        stationRepository.save(farStation);

        // Test geospatial query (5km radius)
        List<Station> nearbyStations = stationRepository.findNearbyStations(
                24.7539, // longitude
                59.4372, // latitude
                5000.0   // radius in meters
        );

        // Verify spatial query works
        assertEquals(1, nearbyStations.size());
        assertEquals("test_station_001", nearbyStations.get(0).getId());
    }
}
package com.onnoto.onnoto_backend.repository;

import com.onnoto.onnoto_backend.model.Network;
import com.onnoto.onnoto_backend.model.Operator;
import com.onnoto.onnoto_backend.model.Station;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class StationRepositoryTest {

    // Mark the PostGIS image as compatible with PostgreSQL
    static DockerImageName postgisImage = DockerImageName.parse("postgis/postgis:15-3.3")
            .asCompatibleSubstituteFor("postgres");

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(postgisImage)
            .withDatabaseName("onnoto-test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);

        // Tell Hibernate to create the schema automatically for tests
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // Set the correct dialect for PostGIS
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.spatial.dialect.postgis.PostgisPG10Dialect");

        // Enable PostGIS extension (PostgreSQL container will handle this)
        registry.add("spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation", () -> "true");

        // Disable Flyway migrations for the test
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Test
    void findByCityShouldReturnStationsInCity() {
        // Given
        Network network = new Network();
        network.setId("elmo");
        network.setName("ELMO");
        network.setCreatedAt(LocalDateTime.now());
        network.setUpdatedAt(LocalDateTime.now());
        networkRepository.save(network);

        Operator operator = new Operator();
        operator.setId("enefit");
        operator.setName("Enefit");
        operator.setCreatedAt(LocalDateTime.now());
        operator.setUpdatedAt(LocalDateTime.now());
        operatorRepository.save(operator);

        Station station1 = new Station();
        station1.setId("elmo_001");
        station1.setName("Tallinn Station");
        station1.setNetwork(network);
        station1.setOperator(operator);
        station1.setLatitude(new BigDecimal("59.4372"));
        station1.setLongitude(new BigDecimal("24.7539"));
        station1.setCity("Tallinn");
        station1.setCreatedAt(LocalDateTime.now());
        station1.setUpdatedAt(LocalDateTime.now());
        stationRepository.save(station1);

        Station station2 = new Station();
        station2.setId("elmo_002");
        station2.setName("Tartu Station");
        station2.setNetwork(network);
        station2.setOperator(operator);
        station2.setLatitude(new BigDecimal("58.3780"));
        station2.setLongitude(new BigDecimal("26.7290"));
        station2.setCity("Tartu");
        station2.setCreatedAt(LocalDateTime.now());
        station2.setUpdatedAt(LocalDateTime.now());
        stationRepository.save(station2);

        // When
        List<Station> tallinnStations = stationRepository.findByCity("Tallinn");

        // Then
        assertNotNull(tallinnStations);
        assertEquals(1, tallinnStations.size());
        assertEquals("Tallinn Station", tallinnStations.get(0).getName());
    }

    @Test
    void findNearbyStationsShouldReturnStationsWithinRadius() {
        // Given
        Network network = createAndSaveTestNetwork();
        Operator operator = createAndSaveTestOperator();

        // Create a station in Tallinn
        Station tallinnStation = new Station();
        tallinnStation.setId("tallinn_station");
        tallinnStation.setName("Tallinn Center");
        tallinnStation.setNetwork(network);
        tallinnStation.setOperator(operator);
        tallinnStation.setLatitude(new BigDecimal("59.4372"));
        tallinnStation.setLongitude(new BigDecimal("24.7539"));
        tallinnStation.setCity("Tallinn");
        tallinnStation.setCreatedAt(LocalDateTime.now());
        tallinnStation.setUpdatedAt(LocalDateTime.now());
        stationRepository.save(tallinnStation);

        // Create a station in Tartu (far from Tallinn)
        Station tartuStation = new Station();
        tartuStation.setId("tartu_station");
        tartuStation.setName("Tartu Center");
        tartuStation.setNetwork(network);
        tartuStation.setOperator(operator);
        tartuStation.setLatitude(new BigDecimal("58.3780"));
        tartuStation.setLongitude(new BigDecimal("26.7290"));
        tartuStation.setCity("Tartu");
        tartuStation.setCreatedAt(LocalDateTime.now());
        tartuStation.setUpdatedAt(LocalDateTime.now());
        stationRepository.save(tartuStation);

        // When - Search near Tallinn with 5km radius
        List<Station> nearbyStations = stationRepository.findNearbyStations(
                24.7539, // longitude
                59.4372, // latitude
                5000.0   // radius in meters
        );

        // Then
        assertNotNull(nearbyStations);
        assertEquals(1, nearbyStations.size());
        assertEquals("Tallinn Center", nearbyStations.get(0).getName());
    }

    private Network createAndSaveTestNetwork() {
        Network network = new Network();
        network.setId("test_network");
        network.setName("Test Network");
        network.setCreatedAt(LocalDateTime.now());
        network.setUpdatedAt(LocalDateTime.now());
        return networkRepository.save(network);
    }

    private Operator createAndSaveTestOperator() {
        Operator operator = new Operator();
        operator.setId("test_operator");
        operator.setName("Test Operator");
        operator.setCreatedAt(LocalDateTime.now());
        operator.setUpdatedAt(LocalDateTime.now());
        return operatorRepository.save(operator);
    }
}
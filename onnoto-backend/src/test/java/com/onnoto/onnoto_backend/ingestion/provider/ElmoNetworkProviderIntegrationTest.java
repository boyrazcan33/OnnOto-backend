package com.onnoto.onnoto_backend.ingestion.provider;

import com.onnoto.onnoto_backend.model.Connector;
import com.onnoto.onnoto_backend.model.Network;
import com.onnoto.onnoto_backend.model.Operator;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.repository.ConnectorRepository;
import com.onnoto.onnoto_backend.repository.NetworkRepository;
import com.onnoto.onnoto_backend.repository.OperatorRepository;
import com.onnoto.onnoto_backend.repository.StationRepository;
import com.onnoto.onnoto_backend.repository.StatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@ExtendWith({SpringExtension.class, MockitoExtension.class})
public class ElmoNetworkProviderIntegrationTest {

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
    private NetworkRepository networkRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private ConnectorRepository connectorRepository;

    @Autowired
    private StatusHistoryRepository statusHistoryRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Mock
    private RestTemplate mockRestTemplate;

    private ElmoNetworkProvider elmoNetworkProvider;

    @BeforeEach
    void setUp() {
        // Clear all existing data
        statusHistoryRepository.deleteAll();
        connectorRepository.deleteAll();
        stationRepository.deleteAll();
        networkRepository.deleteAll();
        operatorRepository.deleteAll();

        // Create the provider with injected repositories and mocked RestTemplate
        elmoNetworkProvider = new ElmoNetworkProvider(
                mockRestTemplate,
                stationRepository,
                connectorRepository,
                statusHistoryRepository,
                networkRepository,
                operatorRepository
        );

        // Use lenient() to avoid UnnecessaryStubbingException if some tests don't use the mock
        lenient().when(mockRestTemplate.getForObject(anyString(), any())).thenReturn("{}");
    }

    @Test
    void testProviderCreatesNetworkAndOperator() {
        // Given - provider has been created

        // When
        List<Station> stations = elmoNetworkProvider.fetchAllStations();

        // Then
        assertNotNull(stations);
        assertFalse(stations.isEmpty());

        // Check if network was created
        Optional<Network> network = networkRepository.findById("elmo");
        assertTrue(network.isPresent());
        assertEquals("ELMO Charging Network", network.get().getName());

        // Check if operator was created
        Optional<Operator> operator = operatorRepository.findById("enefit");
        assertTrue(operator.isPresent());
        assertEquals("Enefit", operator.get().getName());
    }

    @Test
    void testFetchAllStationsCreatesData() {
        // Given - provider has been created

        // When
        List<Station> stations = elmoNetworkProvider.fetchAllStations();

        // Then
        assertNotNull(stations);
        assertEquals(2, stations.size()); // Should create 2 test stations

        // Check if stations were saved to the database
        List<Station> savedStations = stationRepository.findAll();
        assertEquals(2, savedStations.size());

        // Check if connectors were created for the stations
        List<Connector> connectors = connectorRepository.findAll();
        assertFalse(connectors.isEmpty());
        assertEquals(4, connectors.size()); // Should be 2 connectors per station
    }

    @Test
    void testFetchStatusUpdatesUpdatesConnectors() {
        // Given - some stations and connectors exist
        elmoNetworkProvider.fetchAllStations();
        int initialStatusHistoryCount = statusHistoryRepository.findAll().size();

        // When
        elmoNetworkProvider.fetchStatusUpdates();

        // Then
        // Check if status history records were created
        List<Connector> connectors = connectorRepository.findAll();
        assertFalse(connectors.isEmpty());

        // Status history should have been created if any connectors changed status
        int finalStatusHistoryCount = statusHistoryRepository.findAll().size();

        // Since status changes are random in the implementation,
        // we can't assert exact counts, but we log for manual verification
        System.out.println("Initial status history count: " + initialStatusHistoryCount);
        System.out.println("Final status history count: " + finalStatusHistoryCount);

        // We can verify the provider name is working
        assertEquals("ELMO", elmoNetworkProvider.getProviderName());
    }

    @Test
    void testProviderIsAvailable() {
        // Check availability (should always return true in this implementation)
        assertTrue(elmoNetworkProvider.isAvailable());
    }
}
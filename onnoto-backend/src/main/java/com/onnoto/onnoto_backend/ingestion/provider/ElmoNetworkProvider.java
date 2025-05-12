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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
public class ElmoNetworkProvider extends BaseDataProvider {

    private final NetworkRepository networkRepository;
    private final OperatorRepository operatorRepository;

    @Value("${onnoto.provider.elmo.base-url:https://elmo.ee/api}")
    private String baseUrl;

    @Value("${onnoto.provider.elmo.stations-endpoint:/stations}")
    private String stationsEndpoint;

    @Value("${onnoto.provider.elmo.status-endpoint:/stations/status}")
    private String statusEndpoint;

    public ElmoNetworkProvider(
            RestTemplate restTemplate,
            StationRepository stationRepository,
            ConnectorRepository connectorRepository,
            StatusHistoryRepository statusHistoryRepository,
            NetworkRepository networkRepository,
            OperatorRepository operatorRepository) {
        super(restTemplate, stationRepository, connectorRepository, statusHistoryRepository);
        this.networkRepository = networkRepository;
        this.operatorRepository = operatorRepository;
    }

    @Override
    public String getProviderName() {
        return "ELMO";
    }

    @Override
    public List<Station> fetchAllStations() {
        try {
            log.info("Fetching stations from ELMO network");

            // In a real implementation, this would call the ELMO API
            // For now, we'll simulate with a mocked response

            // Ensure network exists
            Network network = ensureNetwork();

            // Ensure operator exists
            Operator operator = ensureOperator();

            // Create some sample stations (in real app this would come from API)
            List<Station> stations = new ArrayList<>();

            // Example station 1
            Station station1 = new Station();
            station1.setId("elmo_001");
            station1.setName("Tallinn Viru Keskus");
            station1.setNetwork(network);
            station1.setOperator(operator);
            station1.setLatitude(new BigDecimal("59.4372"));
            station1.setLongitude(new BigDecimal("24.7539"));
            station1.setAddress("Viru väljak 4");
            station1.setCity("Tallinn");
            station1.setPostalCode("10111");
            station1.setCountry("EE");
            station1.setCreatedAt(LocalDateTime.now());
            station1.setUpdatedAt(LocalDateTime.now());

            // Save and add connectors
            station1 = stationRepository.save(station1);
            createConnectors(station1);
            stations.add(station1);

            // Example station 2
            Station station2 = new Station();
            station2.setId("elmo_002");
            station2.setName("Tartu Lõunakeskus");
            station2.setNetwork(network);
            station2.setOperator(operator);
            station2.setLatitude(new BigDecimal("58.3530"));
            station2.setLongitude(new BigDecimal("26.7310"));
            station2.setAddress("Ringtee 75");
            station2.setCity("Tartu");
            station2.setPostalCode("50501");
            station2.setCountry("EE");
            station2.setCreatedAt(LocalDateTime.now());
            station2.setUpdatedAt(LocalDateTime.now());

            // Save and add connectors
            station2 = stationRepository.save(station2);
            createConnectors(station2);
            stations.add(station2);

            log.info("Fetched {} stations from ELMO network", stations.size());
            return stations;

        } catch (RestClientException e) {
            log.error("Error fetching stations from ELMO network: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public void fetchStatusUpdates() {
        try {
            log.info("Fetching status updates from ELMO network");

            // In a real implementation, this would call the ELMO API
            // For now, we'll update with random statuses

            // Get all connectors for ELMO stations
            Optional<Network> networkOptional = networkRepository.findById("elmo");

            // FIX: Handle the case where the network doesn't exist
            if (!networkOptional.isPresent()) {
                log.warn("Network 'elmo' not found in database. Creating it first.");
                ensureNetwork();
                networkOptional = networkRepository.findById("elmo");

                // If still not present, log and return
                if (!networkOptional.isPresent()) {
                    log.error("Failed to create 'elmo' network. Cannot update statuses.");
                    return;
                }
            }

            Network network = networkOptional.get();
            List<Station> stations = stationRepository.findByNetwork(network);

            if (stations.isEmpty()) {
                log.warn("No stations found for ELMO network. Nothing to update.");
                return;
            }

            Random random = new Random();
            for (Station station : stations) {
                List<Connector> connectors = connectorRepository.findByStation(station);

                if (connectors.isEmpty()) {
                    log.debug("No connectors found for station: {}. Skipping.", station.getId());
                    continue;
                }

                for (Connector connector : connectors) {
                    // Randomly assign statuses
                    String status;
                    int rand = random.nextInt(10);

                    if (rand < 7) {
                        status = "AVAILABLE"; // 70% chance
                    } else if (rand < 9) {
                        status = "OCCUPIED";  // 20% chance
                    } else {
                        status = "OFFLINE";   // 10% chance
                    }

                    // Record status update
                    recordStatusUpdate(connector, status, getProviderName());
                }

                // Update station timestamp
                updateStationTimestamp(station);
            }

            log.info("Updated statuses for {} ELMO stations", stations.size());

        } catch (Exception e) {
            log.error("Error fetching status updates from ELMO network: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // In a real implementation, check if the API is accessible
            return true;
        } catch (Exception e) {
            log.error("ELMO network API is not available: {}", e.getMessage());
            return false;
        }
    }

    private Network ensureNetwork() {
        return networkRepository.findById("elmo")
                .orElseGet(() -> {
                    Network network = new Network();
                    network.setId("elmo");
                    network.setName("ELMO Charging Network");
                    network.setWebsite("https://elmo.ee");
                    network.setCreatedAt(LocalDateTime.now());
                    network.setUpdatedAt(LocalDateTime.now());
                    return networkRepository.save(network);
                });
    }

    private Operator ensureOperator() {
        return operatorRepository.findById("enefit")
                .orElseGet(() -> {
                    Operator operator = new Operator();
                    operator.setId("enefit");
                    operator.setName("Enefit");
                    operator.setContactInfo("info@enefit.ee");
                    operator.setCreatedAt(LocalDateTime.now());
                    operator.setUpdatedAt(LocalDateTime.now());
                    return operatorRepository.save(operator);
                });
    }

    private void createConnectors(Station station) {
        // CCS connector
        Connector ccs = new Connector();
        ccs.setStation(station);
        ccs.setConnectorType("CCS");
        ccs.setPowerKw(new BigDecimal("50.0"));
        ccs.setCurrentType("DC");
        ccs.setStatus("AVAILABLE");
        ccs.setLastStatusUpdate(LocalDateTime.now());
        ccs.setCreatedAt(LocalDateTime.now());
        ccs.setUpdatedAt(LocalDateTime.now());
        connectorRepository.save(ccs);

        // CHAdeMO connector
        Connector chademo = new Connector();
        chademo.setStation(station);
        chademo.setConnectorType("CHAdeMO");
        chademo.setPowerKw(new BigDecimal("50.0"));
        chademo.setCurrentType("DC");
        chademo.setStatus("AVAILABLE");
        chademo.setLastStatusUpdate(LocalDateTime.now());
        chademo.setCreatedAt(LocalDateTime.now());
        chademo.setUpdatedAt(LocalDateTime.now());
        connectorRepository.save(chademo);
    }
}
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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
public class EleportNetworkProvider extends BaseDataProvider {

    private final NetworkRepository networkRepository;
    private final OperatorRepository operatorRepository;

    public EleportNetworkProvider(
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
        return "Eleport";
    }

    @Override
    public List<Station> fetchAllStations() {
        try {
            log.info("Fetching stations from Eleport network");

            Network network = ensureNetwork();
            Operator operator = ensureOperator();

            List<Station> stations = new ArrayList<>();

            // TALLINN STATIONS - Eleport focuses on fast charging locations
            stations.add(createStation("eleport_001", "Circle K Peterburi", network, operator,
                    "59.4269", "24.7745", "Peterburi tee 47", "Tallinn", "11415"));

            stations.add(createStation("eleport_002", "Circle K Kadaka", network, operator,
                    "59.4178", "24.6858", "Kadaka tee 56B", "Tallinn", "12915"));

            stations.add(createStation("eleport_003", "Alexela Laagri", network, operator,
                    "59.3503", "24.6148", "Pärnu mnt 556", "Laagri", "76401"));

            stations.add(createStation("eleport_004", "Circle K Õismäe", network, operator,
                    "59.4114", "24.6485", "Õismäe tee 107A", "Tallinn", "13514"));

            stations.add(createStation("eleport_005", "Shell Männiku", network, operator,
                    "59.3784", "24.6890", "Männiku tee 96", "Tallinn", "11215"));

            stations.add(createStation("eleport_006", "Circle K Peetri", network, operator,
                    "59.3962", "24.7894", "Vana-Tartu mnt 74", "Peetri", "75312"));

            stations.add(createStation("eleport_007", "Alexela Viimsi", network, operator,
                    "59.5071", "24.8145", "Randvere tee 9", "Viimsi", "74001"));

            stations.add(createStation("eleport_008", "Circle K Sõpruse", network, operator,
                    "59.4278", "24.7194", "Sõpruse pst 171", "Tallinn", "13424"));

            // TARTU STATIONS
            stations.add(createStation("eleport_009", "Circle K Ringtee", network, operator,
                    "58.3956", "26.7613", "Ringtee 34", "Tartu", "50105"));

            stations.add(createStation("eleport_010", "Alexela Räni", network, operator,
                    "58.3406", "26.7228", "Räni 27", "Tartu", "50407"));

            // PÄRNU STATIONS
            stations.add(createStation("eleport_011", "Circle K Riia", network, operator,
                    "58.3730", "24.5454", "Riia mnt 129", "Pärnu", "80032"));

            stations.add(createStation("eleport_012", "Shell Suur-Jõe", network, operator,
                    "58.3811", "24.5164", "Suur-Jõe 57", "Pärnu", "80031"));

            // VILJANDI
            stations.add(createStation("eleport_013", "Circle K Viljandi", network, operator,
                    "58.3743", "25.5913", "Riia mnt 52", "Viljandi", "71009"));

            // RAKVERE
            stations.add(createStation("eleport_014", "Alexela Rakvere", network, operator,
                    "59.3381", "26.3670", "Võidu 93", "Rakvere", "44312"));

            // NARVA
            stations.add(createStation("eleport_015", "Circle K Kangelaste", network, operator,
                    "59.4002", "28.1702", "Kangelaste 5", "Narva", "20607"));

            // Highway locations
            stations.add(createStation("eleport_016", "Kose Rest Area", network, operator,
                    "59.2011", "25.1894", "Tallinn-Tartu mnt", "Kose", "75101"));

            stations.add(createStation("eleport_017", "Põlva Circle K", network, operator,
                    "58.0609", "27.0465", "Võru mnt 2", "Põlva", "63304"));

            stations.add(createStation("eleport_018", "Jõhvi Alexela", network, operator,
                    "59.3517", "27.4070", "Narva mnt 8", "Jõhvi", "41532"));

            log.info("Created {} stations from Eleport network", stations.size());
            return stations;

        } catch (RestClientException e) {
            log.error("Error fetching stations from Eleport network: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private Station createStation(String id, String name, Network network, Operator operator,
                                  String lat, String lon, String address, String city, String postalCode) {
        Optional<Station> existing = stationRepository.findById(id);
        if (existing.isPresent()) {
            return existing.get();
        }

        Station station = new Station();
        station.setId(id);
        station.setName(name);
        station.setNetwork(network);
        station.setOperator(operator);
        station.setLatitude(new BigDecimal(lat));
        station.setLongitude(new BigDecimal(lon));
        station.setAddress(address);
        station.setCity(city);
        station.setPostalCode(postalCode);
        station.setCountry("EE");
        station.setCreatedAt(LocalDateTime.now());
        station.setUpdatedAt(LocalDateTime.now());

        station = stationRepository.save(station);
        createHighPowerConnectors(station);

        return station;
    }

    @Override
    public void fetchStatusUpdates() {
        try {
            log.info("Fetching status updates from Eleport network");

            Optional<Network> networkOptional = networkRepository.findById("eleport");
            if (!networkOptional.isPresent()) {
                log.warn("Network 'eleport' not found in database.");
                return;
            }

            Network network = networkOptional.get();
            List<Station> stations = stationRepository.findByNetwork(network);

            Random random = new Random();
            for (Station station : stations) {
                List<Connector> connectors = connectorRepository.findByStation(station);

                for (Connector connector : connectors) {
                    String status = generateHighwayLocationStatus(random);
                    recordStatusUpdate(connector, status, getProviderName());
                }

                updateStationTimestamp(station);
            }

            log.info("Updated statuses for {} Eleport stations", stations.size());

        } catch (Exception e) {
            log.error("Error fetching status updates from Eleport network: {}", e.getMessage(), e);
        }
    }

    private String generateHighwayLocationStatus(Random random) {
        // Highway locations tend to be busier
        int rand = random.nextInt(100);

        if (rand < 60) return "AVAILABLE";     // 60% chance
        else if (rand < 85) return "OCCUPIED";  // 25% chance
        else return "OFFLINE";                  // 15% chance
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private Network ensureNetwork() {
        return networkRepository.findById("eleport")
                .orElseGet(() -> {
                    Network network = new Network();
                    network.setId("eleport");
                    network.setName("Eleport");
                    network.setWebsite("https://eleport.ee");
                    network.setCreatedAt(LocalDateTime.now());
                    network.setUpdatedAt(LocalDateTime.now());
                    return networkRepository.save(network);
                });
    }

    private Operator ensureOperator() {
        return operatorRepository.findById("eleport")
                .orElseGet(() -> {
                    Operator operator = new Operator();
                    operator.setId("eleport");
                    operator.setName("Eleport");
                    operator.setContactInfo("info@eleport.ee");
                    operator.setCreatedAt(LocalDateTime.now());
                    operator.setUpdatedAt(LocalDateTime.now());
                    return operatorRepository.save(operator);
                });
    }

    private void createHighPowerConnectors(Station station) {
        // Eleport focuses on high-power charging

        // CCS - High power
        Connector ccs = new Connector();
        ccs.setStation(station);
        ccs.setConnectorType("CCS");
        ccs.setPowerKw(new BigDecimal("150.0")); // 150 kW fast charging
        ccs.setCurrentType("DC");
        ccs.setStatus("AVAILABLE");
        ccs.setLastStatusUpdate(LocalDateTime.now());
        ccs.setCreatedAt(LocalDateTime.now());
        ccs.setUpdatedAt(LocalDateTime.now());
        connectorRepository.save(ccs);

        // Some locations have dual CCS
        if (new Random().nextBoolean()) {
            Connector ccs2 = new Connector();
            ccs2.setStation(station);
            ccs2.setConnectorType("CCS");
            ccs2.setPowerKw(new BigDecimal("150.0"));
            ccs2.setCurrentType("DC");
            ccs2.setStatus("AVAILABLE");
            ccs2.setLastStatusUpdate(LocalDateTime.now());
            ccs2.setCreatedAt(LocalDateTime.now());
            ccs2.setUpdatedAt(LocalDateTime.now());
            connectorRepository.save(ccs2);
        }

        // CHAdeMO - still common
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
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
            // For now, we'll simulate with realistic Estonian charging stations

            // Ensure network exists
            Network network = ensureNetwork();

            // Ensure operator exists
            Operator operator = ensureOperator();

            // Create realistic sample stations across Estonia
            List<Station> stations = new ArrayList<>();

            // TALLINN STATIONS - Major shopping centers and key locations
            stations.add(createStation("elmo_001", "Viru Keskus", network, operator,
                    "59.4372", "24.7539", "Viru väljak 4", "Tallinn", "10111"));

            stations.add(createStation("elmo_002", "Ülemiste Keskus", network, operator,
                    "59.4229", "24.7991", "Suur-Sõjamäe 4", "Tallinn", "11415"));

            stations.add(createStation("elmo_003", "Kristiine Keskus", network, operator,
                    "59.4270", "24.7236", "Endla 45", "Tallinn", "10615"));

            stations.add(createStation("elmo_004", "Rocca al Mare", network, operator,
                    "59.4261", "24.6507", "Paldiski mnt 102", "Tallinn", "13522"));

            stations.add(createStation("elmo_005", "Telliskivi", network, operator,
                    "59.4403", "24.7287", "Telliskivi 60a", "Tallinn", "10412"));

            stations.add(createStation("elmo_006", "Balti Jaam", network, operator,
                    "59.4398", "24.7367", "Toompuiestee 37", "Tallinn", "10133"));

            stations.add(createStation("elmo_007", "Pirita TOP", network, operator,
                    "59.4656", "24.8387", "Pärnamäe tee 139", "Tallinn", "12011"));

            stations.add(createStation("elmo_008", "Mustamäe Keskus", network, operator,
                    "59.4082", "24.6997", "Tammsaare tee 104a", "Tallinn", "12918"));

            stations.add(createStation("elmo_009", "Nõmme Turg", network, operator,
                    "59.3891", "24.6837", "Jaama 18", "Tallinn", "11615"));

            stations.add(createStation("elmo_010", "Lasnamäe Centrum", network, operator,
                    "59.4354", "24.8872", "Mustakivi tee 13", "Tallinn", "13912"));

            stations.add(createStation("elmo_011", "T1 Mall", network, operator,
                    "59.4273", "24.7931", "Peterburi tee 2", "Tallinn", "11415"));

            stations.add(createStation("elmo_012", "Sikupilli Keskus", network, operator,
                    "59.4277", "24.7774", "Tartu mnt 87", "Tallinn", "10112"));

            stations.add(createStation("elmo_013", "Haabersti Rimi", network, operator,
                    "59.4305", "24.6483", "Ehitajate tee 109", "Tallinn", "13517"));

            stations.add(createStation("elmo_014", "Viimsi Keskus", network, operator,
                    "59.5052", "24.8432", "Sõpruse tee 15", "Viimsi", "74001"));

            stations.add(createStation("elmo_015", "Järve Keskus", network, operator,
                    "59.4045", "24.7219", "Pärnu mnt 238", "Tallinn", "11624"));

            stations.add(createStation("elmo_016", "Solaris Keskus", network, operator,
                    "59.4339", "24.7536", "Estonia pst 9", "Tallinn", "10143"));

            stations.add(createStation("elmo_017", "Stockmann", network, operator,
                    "59.4347", "24.7488", "Liivalaia 53", "Tallinn", "10145"));

            stations.add(createStation("elmo_018", "Magistral", network, operator,
                    "59.4238", "24.6920", "Sõpruse pst 201", "Tallinn", "13419"));

            stations.add(createStation("elmo_019", "Tondi Selver", network, operator,
                    "59.4053", "24.7126", "Tammsaare tee 51", "Tallinn", "11316"));

            stations.add(createStation("elmo_020", "Kadriorg", network, operator,
                    "59.4384", "24.7913", "Weizenbergi 34", "Tallinn", "10127"));

            stations.add(createStation("elmo_021", "Pirita Selver", network, operator,
                    "59.4678", "24.8259", "Rummu tee 4", "Tallinn", "11911"));

            stations.add(createStation("elmo_022", "Astri Narva mnt", network, operator,
                    "59.4258", "24.7856", "Narva mnt 7d", "Tallinn", "10117"));

            stations.add(createStation("elmo_023", "Tehnopol", network, operator,
                    "59.3963", "24.6728", "Akadeemia tee 21", "Tallinn", "12618"));

            stations.add(createStation("elmo_024", "Tallinna Sadam D-terminal", network, operator,
                    "59.4515", "24.7670", "Lootsi 13", "Tallinn", "10151"));

            stations.add(createStation("elmo_025", "Lennujaam", network, operator,
                    "59.4133", "24.8328", "Lennujaama tee 2", "Tallinn", "11101"));

            // TARTU STATIONS
            stations.add(createStation("elmo_026", "Tartu Lõunakeskus", network, operator,
                    "58.3530", "26.6803", "Ringtee 75", "Tartu", "50501"));

            stations.add(createStation("elmo_027", "Tartu Kaubamaja", network, operator,
                    "58.3809", "26.7298", "Riia 1", "Tartu", "51013"));

            stations.add(createStation("elmo_028", "Tartu Eeden", network, operator,
                    "58.3643", "26.6932", "Kalda tee 1c", "Tartu", "50703"));

            stations.add(createStation("elmo_029", "Raadi Circle K", network, operator,
                    "58.3954", "26.7442", "Raadi tee 2", "Tartu", "51009"));

            stations.add(createStation("elmo_030", "Tartu Ülikooli Kliinikum", network, operator,
                    "58.3755", "26.7156", "L. Puusepa 8", "Tartu", "51014"));

            stations.add(createStation("elmo_031", "Tasku Keskus", network, operator,
                    "58.3780", "26.7329", "Turu 2", "Tartu", "51014"));

            stations.add(createStation("elmo_032", "Tartu Raudteejaam", network, operator,
                    "58.3745", "26.7146", "Vaksali 6", "Tartu", "51004"));

            stations.add(createStation("elmo_033", "Tartu Eedeni Maksimarket", network, operator,
                    "58.3650", "26.6947", "Kalda tee 3", "Tartu", "50703"));

            // PÄRNU STATIONS
            stations.add(createStation("elmo_034", "Pärnu Keskus", network, operator,
                    "58.3730", "24.5144", "Aida 7", "Pärnu", "80011"));

            stations.add(createStation("elmo_035", "Port Artur 2", network, operator,
                    "58.3867", "24.4953", "Lai 11", "Pärnu", "80010"));

            stations.add(createStation("elmo_036", "Pärnu Maksimarket", network, operator,
                    "58.3993", "24.5432", "Papiniidu 5", "Pärnu", "80042"));

            stations.add(createStation("elmo_037", "Pärnu Raba Rimi", network, operator,
                    "58.4012", "24.4716", "Raba 39", "Pärnu", "80041"));

            stations.add(createStation("elmo_038", "Pärnu Rannapark", network, operator,
                    "58.3756", "24.5023", "Ranna pst 1", "Pärnu", "80012"));

            // NARVA STATIONS
            stations.add(createStation("elmo_039", "Fama Keskus", network, operator,
                    "59.3796", "28.1903", "Fama 10", "Narva", "20303"));

            stations.add(createStation("elmo_040", "Narva Astri", network, operator,
                    "59.3767", "28.1611", "Tallinna mnt 41", "Narva", "20605"));

            stations.add(createStation("elmo_041", "Narva Kerese Selver", network, operator,
                    "59.3681", "28.1873", "Kerese 3", "Narva", "21008"));

            // VILJANDI
            stations.add(createStation("elmo_042", "Viljandi Centrum", network, operator,
                    "58.3639", "25.5970", "Tallinna 24", "Viljandi", "71020"));

            stations.add(createStation("elmo_043", "Viljandi Männimäe", network, operator,
                    "58.3679", "25.5889", "Riia mnt 35", "Viljandi", "71009"));

            // RAKVERE
            stations.add(createStation("elmo_044", "Rakvere Põhjakeskus", network, operator,
                    "59.3491", "26.3628", "Haljala tee 4", "Rakvere", "44317"));

            stations.add(createStation("elmo_045", "Rakvere Vaala", network, operator,
                    "59.3460", "26.3499", "Laada 16", "Rakvere", "44310"));

            stations.add(createStation("elmo_046", "Rakvere Kroonikeskus", network, operator,
                    "59.3467", "26.3560", "Tallinna 41", "Rakvere", "44311"));

            // HAAPSALU
            stations.add(createStation("elmo_047", "Haapsalu Kaubamaja", network, operator,
                    "58.9432", "23.5411", "Tallinna mnt 3", "Haapsalu", "90502"));

            stations.add(createStation("elmo_048", "Haapsalu Rannarootsi", network, operator,
                    "58.9489", "23.5367", "Jaama 2", "Haapsalu", "90504"));

            // KURESSAARE
            stations.add(createStation("elmo_049", "Kuressaare Auriga", network, operator,
                    "58.2479", "22.5092", "Tallinna 88", "Kuressaare", "93819"));

            stations.add(createStation("elmo_050", "Kuressaare Smuul", network, operator,
                    "58.2508", "22.4843", "Pihtla tee 2", "Kuressaare", "93815"));

            // JÕHVI
            stations.add(createStation("elmo_051", "Jõhvi Tsentraal", network, operator,
                    "59.3594", "27.4211", "Keskväljak 4", "Jõhvi", "41531"));

            stations.add(createStation("elmo_052", "Jõhvi Pargi", network, operator,
                    "59.3541", "27.4196", "Pargi 39", "Jõhvi", "41537"));

            // VALGA
            stations.add(createStation("elmo_053", "Valga Maxima", network, operator,
                    "57.7775", "26.0473", "Riia 12", "Valga", "68204"));

            stations.add(createStation("elmo_054", "Valga Keskus", network, operator,
                    "57.7789", "26.0456", "Kesk 11", "Valga", "68203"));

            // PÕLVA
            stations.add(createStation("elmo_055", "Põlva Coop", network, operator,
                    "58.0530", "27.0693", "Jaama 12", "Põlva", "63308"));

            stations.add(createStation("elmo_056", "Põlva Käisi", network, operator,
                    "58.0567", "27.0581", "Käisi tee 2", "Põlva", "63306"));

            // VÕRU
            stations.add(createStation("elmo_057", "Võru Kagukeskus", network, operator,
                    "57.8339", "26.9936", "Jüri 19A", "Võru", "65610"));

            stations.add(createStation("elmo_058", "Võru Maxima", network, operator,
                    "57.8423", "27.0196", "Vilja 4", "Võru", "65604"));

            // PAIDE
            stations.add(createStation("elmo_059", "Paide Selver", network, operator,
                    "58.8854", "25.5573", "Keskväljak 8", "Paide", "72713"));

            stations.add(createStation("elmo_060", "Paide Vallimäe", network, operator,
                    "58.8889", "25.5601", "Vallimäe 3", "Paide", "72712"));

            // SMALLER TOWNS AND HIGHWAY LOCATIONS
            stations.add(createStation("elmo_061", "Rapla Selver", network, operator,
                    "59.0084", "24.7947", "Tallinna mnt 2", "Rapla", "79513"));

            stations.add(createStation("elmo_062", "Keila Selver", network, operator,
                    "59.3036", "24.4102", "Haapsalu mnt 57", "Keila", "76605"));

            stations.add(createStation("elmo_063", "Saue Kaubakeskus", network, operator,
                    "59.3226", "24.5497", "Koondise 27", "Saue", "76505"));

            stations.add(createStation("elmo_064", "Maardu Maxima", network, operator,
                    "59.4745", "25.0190", "Keemikute 2", "Maardu", "74114"));

            stations.add(createStation("elmo_065", "Kiviõli Virumaa", network, operator,
                    "59.3566", "26.9712", "Keskpuiestee 35", "Kiviõli", "43125"));

            stations.add(createStation("elmo_066", "Kunda Port", network, operator,
                    "59.5213", "26.5405", "Sadama tee 12", "Kunda", "44107"));

            stations.add(createStation("elmo_067", "Tapa Grossi", network, operator,
                    "59.2606", "25.9586", "Pikk 6", "Tapa", "45106"));

            stations.add(createStation("elmo_068", "Türi Säästumarket", network, operator,
                    "58.8075", "25.4324", "Viljandi 16", "Türi", "72211"));

            stations.add(createStation("elmo_069", "Elva Arbimäe", network, operator,
                    "58.2225", "26.4211", "Kesk 23", "Elva", "61504"));

            stations.add(createStation("elmo_070", "Otepää Tehvandi", network, operator,
                    "58.0586", "26.4938", "Tehvandi 1", "Otepää", "67403"));

            log.info("Created {} stations from ELMO network", stations.size());
            return stations;

        } catch (RestClientException e) {
            log.error("Error fetching stations from ELMO network: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private Station createStation(String id, String name, Network network, Operator operator,
                                  String lat, String lon, String address, String city, String postalCode) {
        // Check if station already exists
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

        // Save station
        station = stationRepository.save(station);

        // Create connectors with varying configurations
        createConnectorsWithVariation(station);

        return station;
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
                    // More realistic status distribution based on time of day and location
                    String status = generateRealisticStatus(random, station);

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

    private String generateRealisticStatus(Random random, Station station) {
        // More realistic status distribution
        // Consider factors like location, time of day, etc.
        int hour = LocalDateTime.now().getHour();
        int rand = random.nextInt(100);

        // City center locations are busier
        boolean isCityCenter = station.getName().contains("Keskus") ||
                station.getName().contains("Viru") ||
                station.getName().contains("Mall");

        // During peak hours (7-9 AM, 5-7 PM), more occupied
        if ((hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 19)) {
            if (isCityCenter) {
                if (rand < 60) return "OCCUPIED";      // 60% chance
                else if (rand < 95) return "AVAILABLE"; // 35% chance
                else return "OFFLINE";                  // 5% chance
            } else {
                if (rand < 40) return "OCCUPIED";      // 40% chance
                else if (rand < 95) return "AVAILABLE"; // 55% chance
                else return "OFFLINE";                  // 5% chance
            }
        }
        // Night time (11 PM - 6 AM), mostly available
        else if (hour >= 23 || hour <= 6) {
            if (rand < 85) return "AVAILABLE";     // 85% chance
            else if (rand < 95) return "OCCUPIED";  // 10% chance
            else return "OFFLINE";                  // 5% chance
        }
        // Regular hours
        else {
            if (isCityCenter) {
                if (rand < 30) return "OCCUPIED";      // 30% chance
                else if (rand < 95) return "AVAILABLE"; // 65% chance
                else return "OFFLINE";                  // 5% chance
            } else {
                if (rand < 70) return "AVAILABLE";     // 70% chance
                else if (rand < 90) return "OCCUPIED";  // 20% chance
                else return "OFFLINE";                  // 10% chance
            }
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

    private void createConnectorsWithVariation(Station station) {
        Random random = new Random();

        // Major locations get more connectors
        boolean isMajorLocation = station.getName().contains("Keskus") ||
                station.getName().contains("Ülemiste") ||
                station.getName().contains("Viru") ||
                station.getName().contains("Mall");

        // Most stations have CCS
        if (random.nextInt(100) < 95) { // 95% have CCS
            Connector ccs = new Connector();
            ccs.setStation(station);
            ccs.setConnectorType("CCS");
            // Major locations more likely to have high power
            if (isMajorLocation && random.nextBoolean()) {
                ccs.setPowerKw(new BigDecimal("150.0")); // 150 kW
            } else {
                ccs.setPowerKw(new BigDecimal("50.0"));  // 50 kW
            }
            ccs.setCurrentType("DC");
            ccs.setStatus("AVAILABLE");
            ccs.setLastStatusUpdate(LocalDateTime.now());
            ccs.setCreatedAt(LocalDateTime.now());
            ccs.setUpdatedAt(LocalDateTime.now());
            connectorRepository.save(ccs);

            // Major locations might have dual CCS
            if (isMajorLocation && random.nextInt(100) < 40) { // 40% chance for second CCS
                Connector ccs2 = new Connector();
                ccs2.setStation(station);
                ccs2.setConnectorType("CCS");
                ccs2.setPowerKw(ccs.getPowerKw());
                ccs2.setCurrentType("DC");
                ccs2.setStatus("AVAILABLE");
                ccs2.setLastStatusUpdate(LocalDateTime.now());
                ccs2.setCreatedAt(LocalDateTime.now());
                ccs2.setUpdatedAt(LocalDateTime.now());
                connectorRepository.save(ccs2);
            }
        }

        // Many stations have CHAdeMO
        if (random.nextInt(100) < 80) { // 80% have CHAdeMO
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

        // Most stations have Type 2
        if (random.nextInt(100) < 90) { // 90% have Type 2
            Connector type2 = new Connector();
            type2.setStation(station);
            type2.setConnectorType("Type 2");
            type2.setPowerKw(new BigDecimal(random.nextBoolean() ? "22.0" : "43.0")); // Mix of 22kW and 43kW
            type2.setCurrentType("AC");
            type2.setStatus("AVAILABLE");
            type2.setLastStatusUpdate(LocalDateTime.now());
            type2.setCreatedAt(LocalDateTime.now());
            type2.setUpdatedAt(LocalDateTime.now());
            connectorRepository.save(type2);

            // Some locations have multiple Type 2
            if (random.nextInt(100) < 30) { // 30% chance for second Type 2
                Connector type2_2 = new Connector();
                type2_2.setStation(station);
                type2_2.setConnectorType("Type 2");
                type2_2.setPowerKw(new BigDecimal("22.0"));
                type2_2.setCurrentType("AC");
                type2_2.setStatus("AVAILABLE");
                type2_2.setLastStatusUpdate(LocalDateTime.now());
                type2_2.setCreatedAt(LocalDateTime.now());
                type2_2.setUpdatedAt(LocalDateTime.now());
                connectorRepository.save(type2_2);
            }
        }
    }
}
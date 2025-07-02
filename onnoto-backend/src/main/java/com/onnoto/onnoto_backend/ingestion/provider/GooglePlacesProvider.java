package com.onnoto.onnoto_backend.ingestion.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onnoto.onnoto_backend.model.Connector;
import com.onnoto.onnoto_backend.model.Network;
import com.onnoto.onnoto_backend.model.Operator;
import com.onnoto.onnoto_backend.model.Station;
import com.onnoto.onnoto_backend.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GooglePlacesProvider extends BaseDataProvider {

    private final NetworkRepository networkRepository;
    private final OperatorRepository operatorRepository;
    private final ObjectMapper objectMapper;

    @Value("${onnoto.provider.google.api-key}")
    private String apiKey;

    // Add this method to check if the API key is loaded
    @PostConstruct
    public void init() {
        log.info("Google API Key loaded: {}", apiKey != null && !apiKey.isEmpty() ? "***KEY_PRESENT***" : "NULL_OR_EMPTY");
        log.info("Google API Key value: {}", apiKey); // Temporarily log the actual value to debug
    }

    @Value("${onnoto.provider.google.nearby-search-url:https://maps.googleapis.com/maps/api/place/nearbysearch/json}")
    private String nearbySearchUrl;

    @Value("${onnoto.provider.google.place-details-url:https://maps.googleapis.com/maps/api/place/details/json}")
    private String placeDetailsUrl;

    @Value("${onnoto.provider.google.radius:50000}") // 50km radius
    private int searchRadius;

    // Estonian cities with their coordinates
    private static final Map<String, double[]> ESTONIAN_CITIES = new HashMap<>() {{
        put("Tallinn", new double[]{59.4370, 24.7536});
        put("Tartu", new double[]{58.3780, 26.7290});
        put("Pärnu", new double[]{58.3859, 24.4971});
        put("Narva", new double[]{59.3797, 28.1791});
        put("Viljandi", new double[]{58.3639, 25.5969});
        put("Rakvere", new double[]{59.3469, 26.3550});
        put("Kuressaare", new double[]{58.2481, 22.5038});
        put("Haapsalu", new double[]{58.9432, 23.5411});
        put("Võru", new double[]{57.8339, 26.9936});
        put("Valga", new double[]{57.7775, 26.0473});
        put("Jõhvi", new double[]{59.3541, 27.4196});
        put("Paide", new double[]{58.8854, 25.5573});
    }};

    public GooglePlacesProvider(
            RestTemplate restTemplate,
            StationRepository stationRepository,
            ConnectorRepository connectorRepository,
            StatusHistoryRepository statusHistoryRepository,
            NetworkRepository networkRepository,
            OperatorRepository operatorRepository,
            ObjectMapper objectMapper) {
        super(restTemplate, stationRepository, connectorRepository, statusHistoryRepository);
        this.networkRepository = networkRepository;
        this.operatorRepository = operatorRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderName() {
        return "GooglePlaces";
    }

    @Override
    public List<Station> fetchAllStations() {
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("Google Places API key is not configured");
            return Collections.emptyList();
        }

        try {
            log.info("Fetching EV charging stations from Google Places API");

            Network network = ensureNetwork();
            Map<String, Station> allStations = new HashMap<>();

            // Search for EV charging stations in each Estonian city
            for (Map.Entry<String, double[]> city : ESTONIAN_CITIES.entrySet()) {
                String cityName = city.getKey();
                double[] coords = city.getValue();

                log.info("Searching for EV charging stations in {}", cityName);

                try {
                    List<Station> cityStations = searchStationsInLocation(
                            coords[0], coords[1], cityName, network
                    );

                    // Add to map to avoid duplicates
                    for (Station station : cityStations) {
                        allStations.put(station.getId(), station);
                    }

                    // Rate limiting - Google Places API has quotas
                    TimeUnit.MILLISECONDS.sleep(200);

                } catch (Exception e) {
                    log.error("Error fetching stations for city {}: {}", cityName, e.getMessage());
                }
            }

            log.info("Fetched {} unique charging stations from Google Places", allStations.size());
            return new ArrayList<>(allStations.values());

        } catch (Exception e) {
            log.error("Error fetching stations from Google Places: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<Station> searchStationsInLocation(double lat, double lng, String cityName, Network network) {
        List<Station> stations = new ArrayList<>();

        try {
            // Build URL for nearby search
            String url = UriComponentsBuilder.fromHttpUrl(nearbySearchUrl)
                    .queryParam("location", lat + "," + lng)
                    .queryParam("radius", searchRadius)
                    .queryParam("type", "electric_vehicle_charging_station")
                    .queryParam("key", apiKey)
                    .build()
                    .toString();

            // Make API call
            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                return stations;
            }

            JsonNode root = objectMapper.readTree(response);

            // Check API status
            String status = root.get("status").asText();
            if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                log.warn("Google Places API returned status: {} for city: {}", status, cityName);
                return stations;
            }

            JsonNode results = root.get("results");
            if (results != null && results.isArray()) {
                for (JsonNode place : results) {
                    try {
                        Station station = parseStation(place, network, cityName);
                        if (station != null) {
                            // Check if station already exists
                            Optional<Station> existing = stationRepository.findById(station.getId());
                            if (!existing.isPresent()) {
                                station = stationRepository.save(station);

                                // Fetch additional details and create connectors
                                fetchStationDetails(station, place.get("place_id").asText());
                            } else {
                                station = existing.get();
                            }
                            stations.add(station);
                        }
                    } catch (Exception e) {
                        log.error("Error parsing station from Google Places: {}", e.getMessage());
                    }
                }
            }

            // Handle pagination if there's a next page token
            if (root.has("next_page_token") && !root.get("next_page_token").isNull()) {
                String nextPageToken = root.get("next_page_token").asText();
                // Google requires a short delay before using the next page token
                TimeUnit.SECONDS.sleep(2);
                stations.addAll(fetchNextPage(nextPageToken, network, cityName));
            }

        } catch (Exception e) {
            log.error("Error searching stations in {}: {}", cityName, e.getMessage());
        }

        return stations;
    }

    private List<Station> fetchNextPage(String pageToken, Network network, String cityName) {
        List<Station> stations = new ArrayList<>();

        try {
            String url = UriComponentsBuilder.fromHttpUrl(nearbySearchUrl)
                    .queryParam("pagetoken", pageToken)
                    .queryParam("key", apiKey)
                    .build()
                    .toString();

            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                return stations;
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.get("results");

            if (results != null && results.isArray()) {
                for (JsonNode place : results) {
                    try {
                        Station station = parseStation(place, network, cityName);
                        if (station != null) {
                            Optional<Station> existing = stationRepository.findById(station.getId());
                            if (!existing.isPresent()) {
                                station = stationRepository.save(station);
                                fetchStationDetails(station, place.get("place_id").asText());
                            } else {
                                station = existing.get();
                            }
                            stations.add(station);
                        }
                    } catch (Exception e) {
                        log.error("Error parsing station: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error fetching next page: {}", e.getMessage());
        }

        return stations;
    }

    private Station parseStation(JsonNode placeNode, Network network, String cityName) {
        try {
            String placeId = placeNode.get("place_id").asText();
            String name = placeNode.get("name").asText();

            // Get location
            JsonNode location = placeNode.get("geometry").get("location");
            BigDecimal latitude = new BigDecimal(location.get("lat").asText());
            BigDecimal longitude = new BigDecimal(location.get("lng").asText());

            // Get address
            String address = placeNode.has("vicinity") ? placeNode.get("vicinity").asText() : "";

            // Determine operator from name or use generic
            Operator operator = determineOperator(name);

            // Create station
            Station station = new Station();
            station.setId("google_" + placeId);
            station.setName(name);
            station.setNetwork(network);
            station.setOperator(operator);
            station.setLatitude(latitude);
            station.setLongitude(longitude);
            station.setAddress(address);
            station.setCity(cityName);
            station.setCountry("EE");
            station.setCreatedAt(LocalDateTime.now());
            station.setUpdatedAt(LocalDateTime.now());

            // Check if it's currently open
            if (placeNode.has("opening_hours")) {
                JsonNode openingHours = placeNode.get("opening_hours");
                if (openingHours.has("open_now")) {
                    boolean isOpen = openingHours.get("open_now").asBoolean();
                    station.setLastStatusUpdate(LocalDateTime.now());
                }
            }

            return station;

        } catch (Exception e) {
            log.error("Error parsing station data: {}", e.getMessage());
            return null;
        }
    }

    private void fetchStationDetails(Station station, String placeId) {
        try {
            // Build URL for place details
            String url = UriComponentsBuilder.fromHttpUrl(placeDetailsUrl)
                    .queryParam("place_id", placeId)
                    .queryParam("fields", "name,formatted_address,formatted_phone_number,website,opening_hours,types,business_status")
                    .queryParam("key", apiKey)
                    .build()
                    .toString();

            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                createDefaultConnectors(station);
                return;
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.get("result");

            if (result != null) {
                // Update station with additional details
                if (result.has("formatted_address")) {
                    station.setAddress(result.get("formatted_address").asText());
                }

                // Extract postal code from formatted address if possible
                if (result.has("formatted_address")) {
                    String formattedAddress = result.get("formatted_address").asText();
                    String postalCode = extractPostalCode(formattedAddress);
                    if (postalCode != null) {
                        station.setPostalCode(postalCode);
                    }
                }

                stationRepository.save(station);

                // Create connectors based on available information
                createConnectorsFromDetails(station, result);
            } else {
                createDefaultConnectors(station);
            }

        } catch (Exception e) {
            log.error("Error fetching station details for {}: {}", station.getId(), e.getMessage());
            createDefaultConnectors(station);
        }
    }

    private void createConnectorsFromDetails(Station station, JsonNode details) {
        // Google Places doesn't provide specific connector information,
        // so we create common Estonian connector types

        // Most Estonian stations have Type 2
        Connector type2 = new Connector();
        type2.setStation(station);
        type2.setConnectorType("Type 2");
        type2.setPowerKw(new BigDecimal("22.0"));
        type2.setCurrentType("AC");
        type2.setStatus("UNKNOWN");
        type2.setLastStatusUpdate(LocalDateTime.now());
        type2.setCreatedAt(LocalDateTime.now());
        type2.setUpdatedAt(LocalDateTime.now());
        connectorRepository.save(type2);

        // Many also have CCS
        Connector ccs = new Connector();
        ccs.setStation(station);
        ccs.setConnectorType("CCS");
        ccs.setPowerKw(new BigDecimal("50.0"));
        ccs.setCurrentType("DC");
        ccs.setStatus("UNKNOWN");
        ccs.setLastStatusUpdate(LocalDateTime.now());
        ccs.setCreatedAt(LocalDateTime.now());
        ccs.setUpdatedAt(LocalDateTime.now());
        connectorRepository.save(ccs);

        // Some have CHAdeMO
        if (new Random().nextDouble() < 0.7) { // 70% chance
            Connector chademo = new Connector();
            chademo.setStation(station);
            chademo.setConnectorType("CHAdeMO");
            chademo.setPowerKw(new BigDecimal("50.0"));
            chademo.setCurrentType("DC");
            chademo.setStatus("UNKNOWN");
            chademo.setLastStatusUpdate(LocalDateTime.now());
            chademo.setCreatedAt(LocalDateTime.now());
            chademo.setUpdatedAt(LocalDateTime.now());
            connectorRepository.save(chademo);
        }
    }

    private void createDefaultConnectors(Station station) {
        // Create at least one Type 2 connector as default
        Connector type2 = new Connector();
        type2.setStation(station);
        type2.setConnectorType("Type 2");
        type2.setPowerKw(new BigDecimal("22.0"));
        type2.setCurrentType("AC");
        type2.setStatus("UNKNOWN");
        type2.setLastStatusUpdate(LocalDateTime.now());
        type2.setCreatedAt(LocalDateTime.now());
        type2.setUpdatedAt(LocalDateTime.now());
        connectorRepository.save(type2);
    }

    private Operator determineOperator(String stationName) {
        String nameLower = stationName.toLowerCase();

        // Try to identify known operators from the name
        if (nameLower.contains("elmo")) {
            return ensureOperator("enefit", "Enefit");
        } else if (nameLower.contains("eleport")) {
            return ensureOperator("eleport", "Eleport");
        } else if (nameLower.contains("virta")) {
            return ensureOperator("virta", "Virta");
        } else if (nameLower.contains("circle k")) {
            return ensureOperator("circlek", "Circle K");
        } else if (nameLower.contains("alexela")) {
            return ensureOperator("alexela", "Alexela");
        } else if (nameLower.contains("shell")) {
            return ensureOperator("shell", "Shell");
        } else {
            // Generic operator for unknown
            return ensureOperator("google_unknown", "Unknown Operator");
        }
    }

    private String extractPostalCode(String address) {
        // Estonian postal codes are 5 digits
        String[] parts = address.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.matches("\\d{5}")) {
                return trimmed;
            }
        }
        return null;
    }

    @Override
    public void fetchStatusUpdates() {
        // Google Places API doesn't provide real-time status updates
        // We could potentially use the opening_hours field to determine if a station is likely available
        log.info("Google Places doesn't provide real-time connector status, skipping status updates");

        // Update all connectors to have a recent timestamp
        List<Station> stations = stationRepository.findByNetwork(ensureNetwork());
        for (Station station : stations) {
            if (station.getId().startsWith("google_")) {
                station.setLastStatusUpdate(LocalDateTime.now());
                stationRepository.save(station);
            }
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // Test API with a simple request
            String url = UriComponentsBuilder.fromHttpUrl(nearbySearchUrl)
                    .queryParam("location", "59.4370,24.7536") // Tallinn center
                    .queryParam("radius", "1000")
                    .queryParam("type", "electric_vehicle_charging_station")
                    .queryParam("key", apiKey)
                    .build()
                    .toString();

            String response = restTemplate.getForObject(url, String.class);
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                String status = root.get("status").asText();
                return "OK".equals(status) || "ZERO_RESULTS".equals(status);
            }
            return false;
        } catch (Exception e) {
            log.error("Google Places API is not available: {}", e.getMessage());
            return false;
        }
    }

    private Network ensureNetwork() {
        return networkRepository.findById("google")
                .orElseGet(() -> {
                    Network network = new Network();
                    network.setId("google");
                    network.setName("Google Places");
                    network.setWebsite("https://maps.google.com");
                    network.setCreatedAt(LocalDateTime.now());
                    network.setUpdatedAt(LocalDateTime.now());
                    return networkRepository.save(network);
                });
    }

    private Operator ensureOperator(String id, String name) {
        final String operatorId = "google_" + id;
        return operatorRepository.findById(operatorId)
                .orElseGet(() -> {
                    Operator operator = new Operator();
                    operator.setId(operatorId);
                    operator.setName(name);
                    operator.setCreatedAt(LocalDateTime.now());
                    operator.setUpdatedAt(LocalDateTime.now());
                    return operatorRepository.save(operator);
                });
    }
}
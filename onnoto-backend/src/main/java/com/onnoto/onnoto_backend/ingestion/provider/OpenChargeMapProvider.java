package com.onnoto.onnoto_backend.ingestion.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class OpenChargeMapProvider extends BaseDataProvider {

    private final NetworkRepository networkRepository;
    private final OperatorRepository operatorRepository;
    private final ObjectMapper objectMapper;

    @Value("${onnoto.provider.opencharge.base-url:https://api.openchargemap.io/v3/poi}")
    private String baseUrl;

    @Value("${onnoto.provider.opencharge.country-code:EE}")
    private String countryCode;

    @Value("${onnoto.provider.opencharge.max-results:200}")
    private int maxResults;

    @Value("${onnoto.provider.opencharge.api-key:}")
    private String apiKey;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000; // 2 seconds

    public OpenChargeMapProvider(
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
        return "OpenChargeMap";
    }

    @Override
    public List<Station> fetchAllStations() {
        try {
            log.info("Fetching stations from OpenChargeMap API");

            // Build URL with API key
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(String.format("%s?countrycode=%s&maxresults=%d&output=json&includecomments=false&verbose=false",
                    baseUrl, countryCode, maxResults));

            // Add API key if available
            if (apiKey != null && !apiKey.isEmpty()) {
                urlBuilder.append("&key=").append(apiKey);
            }

            String url = urlBuilder.toString();

            // Log the URL without the API key for security
            log.debug("Requesting OpenChargeMap data from: {}", url.replaceAll("&key=[^&]*", "&key=REDACTED"));

            String response = fetchWithRetry(url);
            if (response == null) {
                log.warn("No response from OpenChargeMap API after retries");
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response);
            List<Station> stations = new ArrayList<>();

            if (root.isArray()) {
                for (JsonNode stationNode : root) {
                    try {
                        Station station = parseStation(stationNode);
                        if (station != null) {
                            // Save station first
                            station = stationRepository.save(station);

                            // Create connectors for this station
                            createConnectorsFromOCM(station, stationNode);
                            stations.add(station);
                        }
                    } catch (Exception e) {
                        log.error("Error parsing station from OpenChargeMap: {}", e.getMessage());
                    }
                }
            }

            log.info("Fetched {} stations from OpenChargeMap", stations.size());
            return stations;

        } catch (RestClientException e) {
            log.error("Error fetching stations from OpenChargeMap: {}", e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error processing OpenChargeMap data: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public void fetchStatusUpdates() {
        try {
            log.info("OpenChargeMap doesn't provide real-time status updates, skipping status update");
            // OpenChargeMap is primarily a directory service and doesn't provide real-time status
            // We could potentially fetch the latest data and check for status changes, but
            // it's not really designed for real-time updates
        } catch (Exception e) {
            log.error("Error in OpenChargeMap status update: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // Build URL with API key
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(String.format("%s?countrycode=%s&maxresults=1", baseUrl, countryCode));

            // Add API key if available
            if (apiKey != null && !apiKey.isEmpty()) {
                urlBuilder.append("&key=").append(apiKey);
            }

            String url = urlBuilder.toString();

            // Log URL without API key
            log.debug("Checking OpenChargeMap availability with: {}",
                    url.replaceAll("&key=[^&]*", "&key=REDACTED"));

            String response = fetchWithRetry(url);
            return response != null && !response.trim().isEmpty();
        } catch (Exception e) {
            log.error("OpenChargeMap API is not available: {}", e.getMessage());
            return false;
        }
    }

    private String fetchWithRetry(String url) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                String response = restTemplate.getForObject(url, String.class);
                if (response != null && !response.trim().isEmpty()) {
                    return response;
                }
                log.warn("Empty response from OpenChargeMap API, attempt {}/{}",
                        attempts + 1, MAX_RETRY_ATTEMPTS);
            } catch (Exception e) {
                lastException = e;
                log.warn("Error fetching from OpenChargeMap API, attempt {}/{}: {}",
                        attempts + 1, MAX_RETRY_ATTEMPTS, e.getMessage());
            }

            attempts++;

            if (attempts < MAX_RETRY_ATTEMPTS) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (lastException != null) {
            log.error("Failed to fetch data from OpenChargeMap API after {} attempts: {}",
                    MAX_RETRY_ATTEMPTS, lastException.getMessage());
        } else {
            log.error("Failed to fetch data from OpenChargeMap API after {} attempts: Empty responses",
                    MAX_RETRY_ATTEMPTS);
        }

        return null;
    }

    private Station parseStation(JsonNode stationNode) {
        try {
            // Get basic station info
            JsonNode addressInfo = stationNode.get("AddressInfo");
            if (addressInfo == null) {
                return null;
            }

            String ocmId = stationNode.get("ID").asText();
            String title = addressInfo.get("Title").asText();

            // Check if we already have this station (by OCM ID)
            String stationId = "ocm_" + ocmId;

            Station existingStation = stationRepository.findById(stationId).orElse(null);
            if (existingStation != null) {
                log.debug("Station {} already exists, skipping", stationId);
                return existingStation;
            }

            // Get coordinates
            BigDecimal latitude = new BigDecimal(addressInfo.get("Latitude").asText());
            BigDecimal longitude = new BigDecimal(addressInfo.get("Longitude").asText());

            // Get or create network
            Network network = getOrCreateNetwork(stationNode);

            // Get or create operator
            Operator operator = getOrCreateOperator(stationNode);

            // Create station
            Station station = new Station();
            station.setId(stationId);
            station.setName(title);
            station.setNetwork(network);
            station.setOperator(operator);
            station.setLatitude(latitude);
            station.setLongitude(longitude);

            // Set address info
            if (addressInfo.has("AddressLine1") && !addressInfo.get("AddressLine1").isNull()) {
                station.setAddress(addressInfo.get("AddressLine1").asText());
            }
            if (addressInfo.has("Town") && !addressInfo.get("Town").isNull()) {
                station.setCity(addressInfo.get("Town").asText());
            }
            if (addressInfo.has("Postcode") && !addressInfo.get("Postcode").isNull()) {
                station.setPostalCode(addressInfo.get("Postcode").asText());
            }
            if (addressInfo.has("Country") && !addressInfo.get("Country").isNull()) {
                JsonNode country = addressInfo.get("Country");
                if (country.has("ISOCode")) {
                    station.setCountry(country.get("ISOCode").asText());
                }
            }

            station.setCreatedAt(LocalDateTime.now());
            station.setUpdatedAt(LocalDateTime.now());

            return station;

        } catch (Exception e) {
            log.error("Error parsing station from OpenChargeMap data: {}", e.getMessage());
            return null;
        }
    }

    private Network getOrCreateNetwork(JsonNode stationNode) {
        String networkName = "Unknown Network";
        String networkId = "unknown";

        // Try to get network from OperatorInfo
        if (stationNode.has("OperatorInfo") && !stationNode.get("OperatorInfo").isNull()) {
            JsonNode operatorInfo = stationNode.get("OperatorInfo");
            if (operatorInfo.has("Title") && !operatorInfo.get("Title").isNull()) {
                networkName = operatorInfo.get("Title").asText();
                networkId = "ocm_net_" + networkName.toLowerCase().replaceAll("[^a-z0-9]", "_");
            }
        }

        final String finalNetworkId = networkId;
        final String finalNetworkName = networkName;

        return networkRepository.findById(finalNetworkId)
                .orElseGet(() -> {
                    Network network = new Network();
                    network.setId(finalNetworkId);
                    network.setName(finalNetworkName);
                    network.setCreatedAt(LocalDateTime.now());
                    network.setUpdatedAt(LocalDateTime.now());
                    return networkRepository.save(network);
                });
    }

    private Operator getOrCreateOperator(JsonNode stationNode) {
        String operatorName = "Unknown Operator";
        String operatorId = "unknown";

        // Try to get operator from OperatorInfo
        if (stationNode.has("OperatorInfo") && !stationNode.get("OperatorInfo").isNull()) {
            JsonNode operatorInfo = stationNode.get("OperatorInfo");
            if (operatorInfo.has("Title") && !operatorInfo.get("Title").isNull()) {
                operatorName = operatorInfo.get("Title").asText();
                operatorId = "ocm_op_" + operatorName.toLowerCase().replaceAll("[^a-z0-9]", "_");
            }
        }

        final String finalOperatorId = operatorId;
        final String finalOperatorName = operatorName;

        return operatorRepository.findById(finalOperatorId)
                .orElseGet(() -> {
                    Operator operator = new Operator();
                    operator.setId(finalOperatorId);
                    operator.setName(finalOperatorName);
                    operator.setCreatedAt(LocalDateTime.now());
                    operator.setUpdatedAt(LocalDateTime.now());
                    return operatorRepository.save(operator);
                });
    }

    private void createConnectorsFromOCM(Station station, JsonNode stationNode) {
        if (!stationNode.has("Connections") || stationNode.get("Connections").isNull()) {
            return;
        }

        JsonNode connections = stationNode.get("Connections");
        if (!connections.isArray()) {
            return;
        }

        for (JsonNode connectionNode : connections) {
            try {
                Connector connector = new Connector();
                connector.setStation(station);

                // Get connector type
                if (connectionNode.has("ConnectionType") && !connectionNode.get("ConnectionType").isNull()) {
                    JsonNode connType = connectionNode.get("ConnectionType");
                    if (connType.has("Title")) {
                        connector.setConnectorType(connType.get("Title").asText());
                    }
                } else {
                    connector.setConnectorType("Unknown");
                }

                // Get power rating
                if (connectionNode.has("PowerKW") && !connectionNode.get("PowerKW").isNull()) {
                    connector.setPowerKw(new BigDecimal(connectionNode.get("PowerKW").asText()));
                }

                // Get current type
                if (connectionNode.has("CurrentType") && !connectionNode.get("CurrentType").isNull()) {
                    JsonNode currentType = connectionNode.get("CurrentType");
                    if (currentType.has("Title")) {
                        connector.setCurrentType(currentType.get("Title").asText());
                    }
                }

                // OpenChargeMap doesn't provide real-time status, so we set a default
                connector.setStatus("UNKNOWN");
                connector.setLastStatusUpdate(LocalDateTime.now());
                connector.setCreatedAt(LocalDateTime.now());
                connector.setUpdatedAt(LocalDateTime.now());

                connectorRepository.save(connector);

            } catch (Exception e) {
                log.error("Error creating connector from OpenChargeMap data: {}", e.getMessage());
            }
        }
    }
}
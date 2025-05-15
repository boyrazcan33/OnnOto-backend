package com.onnoto.onnoto_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.username:default}")
    private String redisUsername;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.apikey:}")
    private String redisApiKey;

    @Value("${spring.redis.ssl:false}")
    private boolean redisSsl;

    @PostConstruct
    public void logRedisConfiguration() {
        log.info("Redis Configuration:");
        log.info("Host: {}", redisHost);
        log.info("Port: {}", redisPort);
        log.info("Username: {}", redisUsername);
        log.info("SSL Enabled: {}", redisSsl);
        log.info("API Key Present: {}", (redisApiKey != null && !redisApiKey.isEmpty()) ? "Yes" : "No");
        log.info("Password Present: {}", (redisPassword != null && !redisPassword.isEmpty()) ? "Yes" : "No");
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        try {
            log.info("Configuring Redis Cloud connection to {}:{} with SSL explicitly disabled", redisHost, redisPort);
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(redisHost);
            config.setPort(redisPort);

            // Set username
            if (redisUsername != null && !redisUsername.isEmpty() && !"default".equals(redisUsername)) {
                config.setUsername(redisUsername);
                log.info("Using Redis username: {}", redisUsername);
            }

            // Set password and API key
            if (redisPassword != null && !redisPassword.isEmpty()) {
                // If password is provided, use it for authentication
                config.setPassword(redisPassword);
                log.info("Using Redis password for authentication");
            } else if (redisApiKey != null && !redisApiKey.isEmpty()) {
                // If API key is provided, use it for authentication
                config.setPassword(redisApiKey);
                log.info("Using Redis API key for authentication");
            } else {
                log.warn("No Redis authentication credentials provided");
            }

            // Create client configuration without SSL
            LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                    // No SSL configuration - explicitly NOT using SSL
                    .build();

            log.info("Redis SSL explicitly disabled");

            return new LettuceConnectionFactory(config, clientConfig);
        } catch (Exception e) {
            log.error("Failed to create Redis connection factory: {}", e.getMessage(), e);
            // Log error but allow application to continue without Redis
            log.warn("Application will continue without Redis. Caching will be disabled.");
            return null;
        }
    }

    @Bean
    public ObjectMapper redisObjectMapper() {
        // Configure ObjectMapper to handle Java 8 date/time types
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        return objectMapper;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        try {
            if (connectionFactory == null) {
                throw new IllegalArgumentException("Redis connection factory is null");
            }

            // Create a serializer that can handle Java 8 date/time types
            GenericJackson2JsonRedisSerializer jsonSerializer =
                    new GenericJackson2JsonRedisSerializer(redisObjectMapper());

            // Default cache configuration
            RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(30))  // Default TTL - 30 minutes
                    .serializeKeysWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(jsonSerializer));

            // Configure specific TTLs for different caches
            Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

            // Station data - longer TTL, changes less frequently
            cacheConfigurations.put("stations", defaultConfig.entryTtl(Duration.ofHours(2)));
            cacheConfigurations.put("stationDetails", defaultConfig.entryTtl(Duration.ofHours(1)));

            // Status data - shorter TTL, changes more frequently
            cacheConfigurations.put("nearbyStations", defaultConfig.entryTtl(Duration.ofMinutes(10)));
            cacheConfigurations.put("connectors", defaultConfig.entryTtl(Duration.ofMinutes(5)));

            // Reliability data - medium TTL, updated daily
            cacheConfigurations.put("reliability", defaultConfig.entryTtl(Duration.ofHours(4)));

            // User preferences - longer TTL, rarely changes
            cacheConfigurations.put("preferences", defaultConfig.entryTtl(Duration.ofDays(1)));

            log.info("Building Redis cache manager");
            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaultConfig)
                    .withInitialCacheConfigurations(cacheConfigurations)
                    .build();
        } catch (Exception e) {
            log.error("Failed to build Redis cache manager: {}", e.getMessage(), e);
            throw e;
        }
    }
}
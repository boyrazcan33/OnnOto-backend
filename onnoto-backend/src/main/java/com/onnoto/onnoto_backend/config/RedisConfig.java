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

    @Value("${spring.redis.ssl:false}")
    private boolean redisSsl;

    @PostConstruct
    public void logRedisConfiguration() {
        log.info("Redis Configuration:");
        log.info("Host: {}", redisHost);
        log.info("Port: {}", redisPort);
        log.info("Username: {}", redisUsername);
        log.info("SSL Enabled: {}", redisSsl);
        log.info("Password Present: {}", (redisPassword != null && !redisPassword.isEmpty()) ? "Yes" : "No");
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Configuring Redis connection to {}:{}", redisHost, redisPort);

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);

        // Set username if provided and not default
        if (redisUsername != null && !redisUsername.isEmpty()) {
            config.setUsername(redisUsername);
            log.info("Using Redis username: {}", redisUsername);
        }

        // Set password if provided
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
            log.info("Using Redis password for authentication");
        } else {
            log.warn("No Redis password provided");
        }

        // Create client configuration - SSL explicitly disabled
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(5))
                .build();

        log.info("Redis SSL explicitly disabled");

        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        return objectMapper;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("stations", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigurations.put("stationDetails", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("nearbyStations", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("connectors", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("reliability", defaultConfig.entryTtl(Duration.ofHours(4)));
        cacheConfigurations.put("preferences", defaultConfig.entryTtl(Duration.ofDays(1)));

        log.info("Building Redis cache manager");
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
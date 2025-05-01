package com.onnoto.onnoto_backend.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    public RedisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public Health health() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = new String(connection.ping());
            return Health.up()
                    .withDetail("ping", pong)
                    .withDetail("version", connection.info().getProperty("redis_version"))
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
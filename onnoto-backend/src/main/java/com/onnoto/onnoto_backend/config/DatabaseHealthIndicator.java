package com.onnoto.onnoto_backend.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result != null && result == 1) {
                return Health.up()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("postgis", checkPostGis())
                        .build();
            } else {
                return Health.down()
                        .withDetail("error", "Database query returned unexpected result")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private boolean checkPostGis() {
        try {
            Integer result = jdbcTemplate.queryForObject(
                    "SELECT 1 FROM pg_extension WHERE extname = 'postgis'",
                    Integer.class);
            return result != null && result == 1;
        } catch (Exception e) {
            return false;
        }
    }
}
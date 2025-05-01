package com.onnoto.onnoto_backend.db.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class TestDataSeeder {
    private static final Logger logger = LoggerFactory.getLogger(TestDataSeeder.class);

    @Bean
    @Profile("dev")  // Only run in dev profile
    @Order(2)  // Run after database initialization
    public CommandLineRunner seedTestData(DataSource dataSource) {
        return args -> {
            logger.info("Checking if test data needs to be seeded...");
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            // Check if data already exists
            Integer networkCount = null;
            try {
                networkCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM networks", Integer.class);

                if (networkCount != null && networkCount > 0) {
                    logger.info("Test data already exists ({} networks found), skipping seeding", networkCount);
                    return;
                }
            } catch (Exception e) {
                logger.warn("Error checking network count, will attempt to seed data: {}", e.getMessage());
            }

            try {
                logger.info("Seeding test data...");
                // Insert networks
                jdbcTemplate.update(
                        "INSERT INTO networks (id, name, website) VALUES (?, ?, ?)",
                        "elmo", "ELMO Charging Network", "https://elmo.ee"
                );

                jdbcTemplate.update(
                        "INSERT INTO networks (id, name, website) VALUES (?, ?, ?)",
                        "eleport", "Eleport", "https://eleport.ee"
                );

                jdbcTemplate.update(
                        "INSERT INTO networks (id, name, website) VALUES (?, ?, ?)",
                        "virta", "Virta Network", "https://virta.ee"
                );

                // Insert operators
                jdbcTemplate.update(
                        "INSERT INTO operators (id, name, contact_info) VALUES (?, ?, ?)",
                        "enefit", "Enefit", "info@enefit.ee"
                );

                jdbcTemplate.update(
                        "INSERT INTO operators (id, name, contact_info) VALUES (?, ?, ?)",
                        "eleport", "Eleport", "info@eleport.ee"
                );

                // Insert stations
                jdbcTemplate.update(
                        "INSERT INTO stations (id, name, operator_id, network_id, latitude, longitude, address, city, postal_code, country) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "elmo_001", "Tallinn Viru Keskus", "enefit", "elmo",
                        59.4372, 24.7539, "Viru väljak 4", "Tallinn", "10111", "EE"
                );

                jdbcTemplate.update(
                        "INSERT INTO stations (id, name, operator_id, network_id, latitude, longitude, address, city, postal_code, country) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "eleport_001", "Ülemiste Keskus", "eleport", "eleport",
                        59.4229, 24.7991, "Suur-Sõjamäe 4", "Tallinn", "11415", "EE"
                );

                // Insert connectors
                jdbcTemplate.update(
                        "INSERT INTO connectors (station_id, connector_type, power_kw, current_type, status) " +
                                "VALUES (?, ?, ?, ?, ?)",
                        "elmo_001", "CCS", 50.0, "DC", "AVAILABLE"
                );

                jdbcTemplate.update(
                        "INSERT INTO connectors (station_id, connector_type, power_kw, current_type, status) " +
                                "VALUES (?, ?, ?, ?, ?)",
                        "elmo_001", "CHAdeMO", 50.0, "DC", "AVAILABLE"
                );

                jdbcTemplate.update(
                        "INSERT INTO connectors (station_id, connector_type, power_kw, current_type, status) " +
                                "VALUES (?, ?, ?, ?, ?)",
                        "eleport_001", "Type 2", 22.0, "AC", "AVAILABLE"
                );

                logger.info("Test data seeded successfully");
            } catch (Exception e) {
                logger.error("Error seeding test data: {}", e.getMessage(), e);
                throw e;
            }
        };
    }
}
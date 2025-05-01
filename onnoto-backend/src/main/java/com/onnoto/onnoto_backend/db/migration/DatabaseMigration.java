package com.onnoto.onnoto_backend.db.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

@Configuration
public class DatabaseMigration {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigration.class);

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)  // Make sure this runs first
    public CommandLineRunner initDatabase(DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                // Enable PostGIS extension
                try (Statement statement = connection.createStatement()) {
                    statement.execute("CREATE EXTENSION IF NOT EXISTS postgis");
                    logger.info("PostGIS extension enabled successfully");
                }

                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

                // List of SQL statements to create tables
                List<String> sqlStatements = Arrays.asList(
                        // Networks table
                        "CREATE TABLE IF NOT EXISTS networks (" +
                                "    id VARCHAR(100) PRIMARY KEY," +
                                "    name VARCHAR(255) NOT NULL," +
                                "    website VARCHAR(255)," +
                                "    created_at TIMESTAMP NOT NULL DEFAULT NOW()," +
                                "    updated_at TIMESTAMP NOT NULL DEFAULT NOW()" +
                                ")",

                        // Operators table
                        "CREATE TABLE IF NOT EXISTS operators (" +
                                "    id VARCHAR(100) PRIMARY KEY," +
                                "    name VARCHAR(255) NOT NULL," +
                                "    contact_info VARCHAR(255)," +
                                "    created_at TIMESTAMP NOT NULL DEFAULT NOW()," +
                                "    updated_at TIMESTAMP NOT NULL DEFAULT NOW()" +
                                ")",

                        // Stations table
                        "CREATE TABLE IF NOT EXISTS stations (" +
                                "    id VARCHAR(100) PRIMARY KEY," +
                                "    name VARCHAR(255) NOT NULL," +
                                "    operator_id VARCHAR(100) REFERENCES operators(id)," +
                                "    network_id VARCHAR(100) REFERENCES networks(id)," +
                                "    latitude DECIMAL(10, 7) NOT NULL," +
                                "    longitude DECIMAL(10, 7) NOT NULL," +
                                "    address VARCHAR(255)," +
                                "    city VARCHAR(100)," +
                                "    postal_code VARCHAR(20)," +
                                "    country VARCHAR(2) DEFAULT 'EE'," +
                                "    last_status_update TIMESTAMP," +
                                "    reliability_score DECIMAL(5, 2)," +
                                "    created_at TIMESTAMP NOT NULL DEFAULT NOW()," +
                                "    updated_at TIMESTAMP NOT NULL DEFAULT NOW()" +
                                ")",

                        // Connectors table
                        "CREATE TABLE IF NOT EXISTS connectors (" +
                                "    id SERIAL PRIMARY KEY," +
                                "    station_id VARCHAR(100) REFERENCES stations(id)," +
                                "    connector_type VARCHAR(50) NOT NULL," +
                                "    power_kw DECIMAL(6, 2)," +
                                "    current_type VARCHAR(20)," +
                                "    status VARCHAR(50)," +
                                "    last_status_update TIMESTAMP," +
                                "    created_at TIMESTAMP NOT NULL DEFAULT NOW()," +
                                "    updated_at TIMESTAMP NOT NULL DEFAULT NOW()" +
                                ")",

                        // Status History table
                        "CREATE TABLE IF NOT EXISTS status_history (" +
                                "    id SERIAL PRIMARY KEY," +
                                "    station_id VARCHAR(100) REFERENCES stations(id)," +
                                "    connector_id INTEGER REFERENCES connectors(id)," +
                                "    status VARCHAR(50) NOT NULL," +
                                "    source VARCHAR(50) NOT NULL," +
                                "    recorded_at TIMESTAMP NOT NULL DEFAULT NOW()" +
                                ")",

                        // Anonymous Users table
                        "CREATE TABLE IF NOT EXISTS anonymous_users (" +
                                "    device_id VARCHAR(255) PRIMARY KEY," +
                                "    first_seen TIMESTAMP NOT NULL DEFAULT NOW()," +
                                "    last_seen TIMESTAMP NOT NULL DEFAULT NOW()," +
                                "    language_preference VARCHAR(10) DEFAULT 'et'" +
                                ")",

                        // User Preferences table
                        "CREATE TABLE IF NOT EXISTS user_preferences (" +
                                "    id SERIAL PRIMARY KEY," +
                                "    device_id VARCHAR(255) REFERENCES anonymous_users(device_id)," +
                                "    preference_key VARCHAR(100) NOT NULL," +
                                "    preference_value TEXT NOT NULL," +
                                "    created_at TIMESTAMP NOT NULL DEFAULT NOW()," +
                                "    updated_at TIMESTAMP NOT NULL DEFAULT NOW()," +
                                "    UNIQUE(device_id, preference_key)" +
                                ")",

                        // Favorite Stations table
                        "CREATE TABLE IF NOT EXISTS favorite_stations (" +
                                "    id SERIAL PRIMARY KEY," +
                                "    device_id VARCHAR(255) REFERENCES anonymous_users(device_id)," +
                                "    station_id VARCHAR(100) REFERENCES stations(id)," +
                                "    created_at TIMESTAMP NOT NULL DEFAULT NOW()," +
                                "    UNIQUE(device_id, station_id)" +
                                ")",

                        // Reports table
                        "CREATE TABLE IF NOT EXISTS reports (" +
                                "    id SERIAL PRIMARY KEY," +
                                "    station_id VARCHAR(100) REFERENCES stations(id)," +
                                "    device_id VARCHAR(255) REFERENCES anonymous_users(device_id)," +
                                "    report_type VARCHAR(50) NOT NULL," +
                                "    description TEXT," +
                                "    status VARCHAR(50) DEFAULT 'pending'," +
                                "    created_at TIMESTAMP NOT NULL DEFAULT NOW()" +
                                ")",

                        // Reliability Metrics table
                        "CREATE TABLE IF NOT EXISTS reliability_metrics (" +
                                "    id SERIAL PRIMARY KEY," +
                                "    station_id VARCHAR(100) REFERENCES stations(id)," +
                                "    uptime_percentage DECIMAL(5, 2)," +
                                "    report_count INTEGER DEFAULT 0," +
                                "    average_report_severity DECIMAL(3, 2)," +
                                "    last_downtime TIMESTAMP," +
                                "    downtime_frequency DECIMAL(5, 2)," +
                                "    sample_size INTEGER," +
                                "    created_at TIMESTAMP NOT NULL DEFAULT NOW()," +
                                "    updated_at TIMESTAMP NOT NULL DEFAULT NOW()" +
                                ")",

                        // Anomalies table
                        "CREATE TABLE IF NOT EXISTS anomalies (" +
                                "    id SERIAL PRIMARY KEY," +
                                "    station_id VARCHAR(100) REFERENCES stations(id)," +
                                "    anomaly_type VARCHAR(50) NOT NULL," +
                                "    description TEXT," +
                                "    severity DECIMAL(3, 2)," +
                                "    is_resolved BOOLEAN DEFAULT FALSE," +
                                "    detected_at TIMESTAMP NOT NULL DEFAULT NOW()," +
                                "    resolved_at TIMESTAMP" +
                                ")"
                );

                // Execute each SQL statement
                for (String sql : sqlStatements) {
                    try {
                        jdbcTemplate.execute(sql);
                        logger.info("Executed SQL: {}", sql.substring(0, Math.min(sql.length(), 50)) + "...");
                    } catch (Exception e) {
                        logger.error("Error executing SQL: {}", sql, e);
                        throw e;
                    }
                }

                logger.info("Database initialization completed successfully");
            } catch (Exception e) {
                logger.error("Error initializing database: {}", e.getMessage(), e);
                throw e;
            }
        };
    }
}
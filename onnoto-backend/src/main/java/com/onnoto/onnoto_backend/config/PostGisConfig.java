package com.onnoto.onnoto_backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class PostGisConfig {

    @Bean
    public CommandLineRunner enablePostgis(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS postgis");
        };
    }
}
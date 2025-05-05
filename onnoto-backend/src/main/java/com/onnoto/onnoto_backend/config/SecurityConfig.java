package com.onnoto.onnoto_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final DeviceIdAuthFilter deviceIdAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(DeviceIdAuthFilter deviceIdAuthFilter,
                          CorsConfigurationSource corsConfigurationSource) {
        this.deviceIdAuthFilter = deviceIdAuthFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - allow read-only access
                        .requestMatchers(HttpMethod.GET, "/api/stations/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/connectors/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reliability/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/locations/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/visualizations/**").permitAll()
                        .requestMatchers("/api/health/**").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Write operations and sensitive data require device authentication
                        .requestMatchers(HttpMethod.POST, "/api/stations/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/stations/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/stations/**").authenticated()
                        .requestMatchers("/api/reports/**").authenticated()
                        .requestMatchers("/api/preferences/**").authenticated()
                        .requestMatchers("/api/anomalies/**").authenticated()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // Default deny
                        .anyRequest().authenticated()
                )
                .addFilterBefore(deviceIdAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
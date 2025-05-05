package com.onnoto.onnoto_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final DeviceIdAuthFilter deviceIdAuthFilter;
<<<<<<< HEAD

    public SecurityConfig(DeviceIdAuthFilter deviceIdAuthFilter) {
        this.deviceIdAuthFilter = deviceIdAuthFilter;
=======
    private final CorsConfigurationSource corsConfigurationSource; // Inject the bean


    public SecurityConfig(DeviceIdAuthFilter deviceIdAuthFilter,
                          CorsConfigurationSource corsConfigurationSource) {
        this.deviceIdAuthFilter = deviceIdAuthFilter;
        this.corsConfigurationSource = corsConfigurationSource;
>>>>>>> 5134ad8 (cors problem solved ,env added)
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
<<<<<<< HEAD
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
=======
                .cors(cors -> cors.configurationSource(corsConfigurationSource))                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
>>>>>>> 5134ad8 (cors problem solved ,env added)
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

<<<<<<< HEAD
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "https://onnoto.ee",
                "https://www.onnoto.ee"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Device-ID"));
        configuration.setExposedHeaders(Arrays.asList("X-Device-ID"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
=======
>>>>>>> 5134ad8 (cors problem solved ,env added)
}
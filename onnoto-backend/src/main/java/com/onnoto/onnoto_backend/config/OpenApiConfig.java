package com.onnoto.onnoto_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI onnotoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OnnOto API")
                        .description("API for the OnnOto Estonian EV Charging Station Reliability Application")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("OnnOto Support")
                                .email("support@onnoto.com"))
                        .license(new License()
                                .name("Proprietary")))
                .servers(List.of(
                        new Server().url("http://localhost:8087").description("Development Server"),
                        new Server().url("https://api.onnoto.com").description("Production Server")
                ));
    }
}
package com.sigpqr.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SIGPQRv2 User Service API")
                        .version("1.0.0")
                        .description("User lifecycle management — CRUD, profile management, role promotion/demotion"))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local development")
                ));
    }
}

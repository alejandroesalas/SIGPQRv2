package com.sigpqr.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SIGPQRv2 Auth Service API")
                        .version("1.0.0")
                        .description("Authentication and authorization service — password reset, OAuth2 token management"))
                .servers(List.of(
                        new Server().url("http://localhost:9000").description("Local development")
                ));
    }
}

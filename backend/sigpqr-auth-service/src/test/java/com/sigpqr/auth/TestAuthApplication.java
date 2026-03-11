package com.sigpqr.auth;

import com.sigpqr.auth.config.AuthorizationServerConfig;
import com.sigpqr.auth.config.RegisteredClientInitializer;
import com.sigpqr.auth.config.ResourceServerConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.server.servlet.OAuth2AuthorizationServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.server.servlet.OAuth2AuthorizationServerJwtAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(exclude = {
        OAuth2AuthorizationServerAutoConfiguration.class,
        OAuth2AuthorizationServerJwtAutoConfiguration.class
})
@ComponentScan(
        basePackages = {"com.sigpqr.auth", "com.sigpqr.common"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        AuthorizationServerConfig.class,
                        ResourceServerConfig.class,
                        RegisteredClientInitializer.class,
                        AuthServiceApplication.class
                }
        )
)
public class TestAuthApplication {
}

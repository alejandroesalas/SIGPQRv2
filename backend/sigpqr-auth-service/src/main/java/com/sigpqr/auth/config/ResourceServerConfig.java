package com.sigpqr.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for REST API endpoints ({@code /api/**}).
 *
 * <p>Runs at {@code @Order(3)}, after the authorization server and default
 * login filter chains. Password reset endpoints are public; all other
 * {@code /api/**} paths require a valid JWT.</p>
 */
@Configuration
public class ResourceServerConfig {

    /**
     * Filter chain scoped to {@code /api/**} requests.
     *
     * <p>Permits unauthenticated access to:
     * <ul>
     *   <li>{@code POST /api/auth/password/reset-request}</li>
     *   <li>{@code POST /api/auth/password/reset}</li>
     * </ul>
     * All other {@code /api/**} endpoints require authentication.
     * CSRF is disabled because these are stateless REST endpoints.</p>
     *
     * @param http the {@link HttpSecurity} builder
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    @Order(2)
    public SecurityFilterChain resourceServerFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/auth/password/reset-request",
                                "/api/auth/password/reset"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}

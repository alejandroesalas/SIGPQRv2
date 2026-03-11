package com.sigpqr.user.config;

import com.sigpqr.common.constants.SecurityConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        // Swagger / Actuator
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/v3/api-docs/**",
                                "/actuator/**"
                        ).permitAll()

                        // Admin-only teacher creation
                        .requestMatchers(HttpMethod.POST, "/api/users/teachers").hasAuthority("SCOPE_" + SecurityConstants.SCOPE_ADMIN_WRITE)

                        // Public student registration
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

                        // Internal endpoints (service-to-service, no JWT — protected at network level)
                        .requestMatchers("/api/users/internal/**").permitAll()

                        // Admin read endpoints
                        .requestMatchers(HttpMethod.GET, "/api/users").hasAuthority("SCOPE_" + SecurityConstants.SCOPE_ADMIN_READ)
                        .requestMatchers(HttpMethod.GET, "/api/users/count").hasAuthority("SCOPE_" + SecurityConstants.SCOPE_ADMIN_READ)
                        .requestMatchers(HttpMethod.GET, "/api/users/profiles").hasAuthority("SCOPE_" + SecurityConstants.SCOPE_ADMIN_READ)

                        // Admin write endpoints
                        .requestMatchers(HttpMethod.DELETE, "/api/users/{id}").hasAuthority("SCOPE_" + SecurityConstants.SCOPE_ADMIN_WRITE)
                        .requestMatchers(HttpMethod.POST, "/api/users/{id}/restore").hasAuthority("SCOPE_" + SecurityConstants.SCOPE_ADMIN_WRITE)

                        // User manage endpoints
                        .requestMatchers(HttpMethod.PUT, "/api/users/{id}/promote").hasAuthority("SCOPE_" + SecurityConstants.SCOPE_USER_MANAGE)
                        .requestMatchers(HttpMethod.PUT, "/api/users/{id}/demote").hasAuthority("SCOPE_" + SecurityConstants.SCOPE_USER_MANAGE)

                        // User detail/update — authenticated (ownership check done in service if needed)
                        .requestMatchers(HttpMethod.GET, "/api/users/{id}").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/{id}").authenticated()

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));

        return http.build();
    }
}

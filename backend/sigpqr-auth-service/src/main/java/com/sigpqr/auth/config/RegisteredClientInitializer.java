package com.sigpqr.auth.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Seeds the OAuth2 registered client repository on application startup.
 *
 * <p>Registers three SPA clients (authorization_code + PKCE, no client secret)
 * and six service-to-service clients (client_credentials with a shared secret).
 * The initializer is idempotent — existing clients are skipped.</p>
 *
 * <ul>
 *   <li><b>SPA clients:</b> sigpqr-web-student, sigpqr-web-admin, sigpqr-web-coordinator</li>
 *   <li><b>Service clients:</b> sigpqr-user-service, sigpqr-pqr-service, sigpqr-academic-service,
 *       sigpqr-notification-service, sigpqr-file-service, sigpqr-api-gateway</li>
 * </ul>
 */
@Component
public class RegisteredClientInitializer implements CommandLineRunner {

    private final RegisteredClientRepository registeredClientRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisteredClientInitializer(RegisteredClientRepository registeredClientRepository,
                                       PasswordEncoder passwordEncoder) {
        this.registeredClientRepository = registeredClientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers all OAuth2 clients if they do not already exist.
     *
     * @param args command-line arguments (unused)
     */
    @Override
    public void run(String... args) {
        // SPA Clients (Authorization Code + PKCE)
        registerSpaClient("sigpqr-web-student", "Student Web App",
                "http://localhost:3000/callback", "pqr:read", "pqr:write", "profile:read", "profile:write");
        registerSpaClient("sigpqr-web-admin", "Admin Web App",
                "http://localhost:3001/callback", "pqr:read", "pqr:write", "pqr:respond", "profile:read", "profile:write", "admin:read", "admin:write", "user:manage");
        registerSpaClient("sigpqr-web-coordinator", "Coordinator Web App",
                "http://localhost:3002/callback", "pqr:read", "pqr:write", "pqr:respond", "profile:read", "profile:write", "admin:read");

        // Service Clients (Client Credentials)
        registerServiceClient("sigpqr-user-service", "user-service-secret", "user:manage", "profile:read");
        registerServiceClient("sigpqr-pqr-service", "pqr-service-secret", "pqr:read", "pqr:write", "profile:read");
        registerServiceClient("sigpqr-academic-service", "academic-service-secret", "profile:read");
        registerServiceClient("sigpqr-notification-service", "notification-service-secret", "profile:read");
        registerServiceClient("sigpqr-file-service", "file-service-secret", "pqr:read");
        registerServiceClient("sigpqr-api-gateway", "gateway-secret", "profile:read");
    }

    /**
     * Registers a public SPA client using authorization_code + PKCE grant.
     *
     * <p>Access tokens live for 30 minutes; refresh tokens for 8 hours
     * with rotation enabled (no reuse).</p>
     *
     * @param clientId    unique client identifier
     * @param clientName  human-readable client name
     * @param redirectUri OAuth2 redirect URI for the callback
     * @param scopes      scopes granted to this client
     */
    private void registerSpaClient(String clientId, String clientName, String redirectUri, String... scopes) {
        if (registeredClientRepository.findByClientId(clientId) != null) {
            return;
        }

        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientName(clientName)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(redirectUri)
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE);

        for (String scope : scopes) {
            builder.scope(scope);
        }

        builder.clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(true)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(30))
                        .refreshTokenTimeToLive(Duration.ofHours(8))
                        .reuseRefreshTokens(false)
                        .build());

        registeredClientRepository.save(builder.build());
    }

    /**
     * Registers a confidential service client using client_credentials grant.
     *
     * <p>Authenticates via HTTP Basic (client_secret_basic).
     * Access tokens live for 15 minutes.</p>
     *
     * @param clientId     unique client identifier
     * @param clientSecret plain-text secret (BCrypt-hashed before storage)
     * @param scopes       scopes granted to this client
     */
    private void registerServiceClient(String clientId, String clientSecret, String... scopes) {
        if (registeredClientRepository.findByClientId(clientId) != null) {
            return;
        }

        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode(clientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);

        for (String scope : scopes) {
            builder.scope(scope);
        }

        builder.tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(15))
                .build());

        registeredClientRepository.save(builder.build());
    }
}

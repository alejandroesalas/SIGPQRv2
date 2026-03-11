package com.sigpqr.auth;

import com.sigpqr.auth.dto.UserCredentialsDto;
import com.sigpqr.auth.entity.PasswordResetToken;
import com.sigpqr.auth.enums.TokenStatus;
import com.sigpqr.auth.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class PasswordResetIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    private static final String EMAIL = "reset@test.com";
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
    }

    private HttpEntity<Map<String, String>> jsonRequest(Map<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Test
    @DisplayName("requestReset - full flow - token persisted with ACTIVE status")
    void requestReset_fullFlow_tokenPersisted() {
        when(userServiceClient.findByEmail(EMAIL))
                .thenReturn(new UserCredentialsDto(USER_ID, EMAIL, "hash", "STUDENT", true, true));

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/password/reset-request",
                jsonRequest(Map.of("email", EMAIL)),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Optional<PasswordResetToken> saved = tokenRepository.findByEmailAndStatus(EMAIL, TokenStatus.ACTIVE);
        assertThat(saved).isPresent();
        assertThat(saved.get().getUserId()).isEqualTo(USER_ID);
        assertThat(saved.get().getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("resetPassword - full flow - token marked USED")
    void resetPassword_fullFlow_tokenMarkedUsed() {
        doNothing().when(userServiceClient).updatePassword(USER_ID.toString(), "encodedPw");

        PasswordResetToken token = new PasswordResetToken();
        token.setToken("reset-token-123");
        token.setEmail(EMAIL);
        token.setUserId(USER_ID);
        token.setStatus(TokenStatus.ACTIVE);
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        tokenRepository.save(token);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/password/reset",
                jsonRequest(Map.of("token", "reset-token-123", "newPassword", "NewPassword123")),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        PasswordResetToken updated = tokenRepository.findByToken("reset-token-123").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(TokenStatus.USED);
    }

    @Test
    @DisplayName("resetPassword - expired token - returns 422")
    void resetPassword_expiredToken_returns422() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("expired-token-123");
        token.setEmail(EMAIL);
        token.setUserId(USER_ID);
        token.setStatus(TokenStatus.ACTIVE);
        token.setExpiresAt(LocalDateTime.now().minusHours(1));
        tokenRepository.save(token);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/password/reset",
                jsonRequest(Map.of("token", "expired-token-123", "newPassword", "NewPassword123")),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("requestReset - unknown email - returns 404")
    void requestReset_unknownEmail_returns404() {
        when(userServiceClient.findByEmail("nobody@test.com")).thenReturn(null);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/password/reset-request",
                jsonRequest(Map.of("email", "nobody@test.com")),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}

package com.sigpqr.auth;

import com.sigpqr.auth.entity.EmailVerificationToken;
import com.sigpqr.auth.enums.VerificationStatus;
import com.sigpqr.auth.repository.EmailVerificationTokenRepository;
import com.sigpqr.auth.service.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;

import com.sigpqr.common.exception.BusinessRuleException;
import com.sigpqr.common.exception.ResourceNotFoundException;

class EmailVerificationIntegrationTest extends IntegrationTestBase {

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    private static final String EMAIL = "verify@test.com";
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
    }

    @Test
    @DisplayName("createAndVerify - full flow - token marked VERIFIED")
    void createAndVerify_fullFlow_tokenMarkedVerified() {
        doNothing().when(userServiceClient).verifyEmail(USER_ID.toString());

        String tokenValue = emailVerificationService.createToken(EMAIL, USER_ID);

        assertThat(tokenValue).isNotBlank();
        assertThat(tokenRepository.findByToken(tokenValue)).isPresent();

        emailVerificationService.verify(tokenValue);

        EmailVerificationToken updated = tokenRepository.findByToken(tokenValue).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(VerificationStatus.VERIFIED);
    }

    @Test
    @DisplayName("verify - expired token - throws BusinessRuleException")
    void verify_expiredToken_throwsBusinessRule() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken("expired-verify-token");
        token.setEmail(EMAIL);
        token.setUserId(USER_ID);
        token.setStatus(VerificationStatus.ACTIVE);
        token.setExpiresAt(LocalDateTime.now().minusHours(1));
        tokenRepository.save(token);

        assertThatThrownBy(() -> emailVerificationService.verify("expired-verify-token"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("expired");

        // The @Transactional method rolls back on RuntimeException,
        // so the EXPIRED status update is not persisted
        EmailVerificationToken updated = tokenRepository.findByToken("expired-verify-token").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(VerificationStatus.ACTIVE);
    }

    @Test
    @DisplayName("verify - invalid token - throws ResourceNotFoundException")
    void verify_invalidToken_throwsResourceNotFound() {
        assertThatThrownBy(() -> emailVerificationService.verify("nonexistent-token"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

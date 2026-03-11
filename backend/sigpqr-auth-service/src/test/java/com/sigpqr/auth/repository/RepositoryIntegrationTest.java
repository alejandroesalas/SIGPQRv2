package com.sigpqr.auth.repository;

import com.sigpqr.auth.IntegrationTestBase;
import com.sigpqr.auth.entity.EmailVerificationToken;
import com.sigpqr.auth.entity.PasswordResetToken;
import com.sigpqr.auth.enums.TokenStatus;
import com.sigpqr.auth.enums.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
    }

    @Test
    @DisplayName("passwordResetRepo - findByToken - returns token")
    void passwordResetRepo_findByToken_returnsToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("pr-token-1");
        token.setEmail("pr@test.com");
        token.setUserId(USER_ID);
        token.setStatus(TokenStatus.ACTIVE);
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        passwordResetTokenRepository.save(token);

        Optional<PasswordResetToken> found = passwordResetTokenRepository.findByToken("pr-token-1");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("pr@test.com");
        assertThat(found.get().getId()).isNotNull();
    }

    @Test
    @DisplayName("passwordResetRepo - findByEmailAndStatus - filters correctly")
    void passwordResetRepo_findByEmailAndStatus_filtersCorrectly() {
        PasswordResetToken active = new PasswordResetToken();
        active.setToken("pr-active");
        active.setEmail("filter@test.com");
        active.setUserId(USER_ID);
        active.setStatus(TokenStatus.ACTIVE);
        active.setExpiresAt(LocalDateTime.now().plusHours(1));
        passwordResetTokenRepository.save(active);

        PasswordResetToken used = new PasswordResetToken();
        used.setToken("pr-used");
        used.setEmail("filter@test.com");
        used.setUserId(USER_ID);
        used.setStatus(TokenStatus.USED);
        used.setExpiresAt(LocalDateTime.now().plusHours(1));
        passwordResetTokenRepository.save(used);

        Optional<PasswordResetToken> result = passwordResetTokenRepository
                .findByEmailAndStatus("filter@test.com", TokenStatus.ACTIVE);

        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo("pr-active");

        Optional<PasswordResetToken> noResult = passwordResetTokenRepository
                .findByEmailAndStatus("filter@test.com", TokenStatus.EXPIRED);
        assertThat(noResult).isEmpty();
    }

    @Test
    @DisplayName("emailVerificationRepo - findByToken - returns token")
    void emailVerificationRepo_findByToken_returnsToken() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken("ev-token-1");
        token.setEmail("ev@test.com");
        token.setUserId(USER_ID);
        token.setStatus(VerificationStatus.ACTIVE);
        token.setExpiresAt(LocalDateTime.now().plusHours(24));
        emailVerificationTokenRepository.save(token);

        Optional<EmailVerificationToken> found = emailVerificationTokenRepository.findByToken("ev-token-1");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("ev@test.com");
        assertThat(found.get().getId()).isNotNull();
    }

    @Test
    @DisplayName("emailVerificationRepo - findByEmailAndStatus - filters correctly")
    void emailVerificationRepo_findByEmailAndStatus_filtersCorrectly() {
        EmailVerificationToken active = new EmailVerificationToken();
        active.setToken("ev-active");
        active.setEmail("evfilter@test.com");
        active.setUserId(USER_ID);
        active.setStatus(VerificationStatus.ACTIVE);
        active.setExpiresAt(LocalDateTime.now().plusHours(24));
        emailVerificationTokenRepository.save(active);

        EmailVerificationToken verified = new EmailVerificationToken();
        verified.setToken("ev-verified");
        verified.setEmail("evfilter@test.com");
        verified.setUserId(USER_ID);
        verified.setStatus(VerificationStatus.VERIFIED);
        verified.setExpiresAt(LocalDateTime.now().plusHours(24));
        emailVerificationTokenRepository.save(verified);

        Optional<EmailVerificationToken> result = emailVerificationTokenRepository
                .findByEmailAndStatus("evfilter@test.com", VerificationStatus.ACTIVE);

        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo("ev-active");

        Optional<EmailVerificationToken> noResult = emailVerificationTokenRepository
                .findByEmailAndStatus("evfilter@test.com", VerificationStatus.EXPIRED);
        assertThat(noResult).isEmpty();
    }
}

package com.sigpqr.auth.service;

import com.sigpqr.auth.client.UserServiceClient;
import com.sigpqr.auth.entity.EmailVerificationToken;
import com.sigpqr.auth.enums.VerificationStatus;
import com.sigpqr.auth.repository.EmailVerificationTokenRepository;
import com.sigpqr.common.exception.BusinessRuleException;
import com.sigpqr.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;
    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private EmailVerificationService service;

    private static final String EMAIL = "student@university.edu";
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("createToken - new email - saves and returns token")
    void createToken_newEmail_savesAndReturnsToken() {
        when(tokenRepository.findByEmailAndStatus(EMAIL, VerificationStatus.ACTIVE)).thenReturn(Optional.empty());
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(inv -> inv.getArgument(0));

        String token = service.createToken(EMAIL, USER_ID);

        assertThat(token).isNotBlank();
        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        EmailVerificationToken saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getStatus()).isEqualTo(VerificationStatus.ACTIVE);
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("createToken - existing active token - invalidates previous")
    void createToken_existingActiveToken_invalidatesPrevious() {
        EmailVerificationToken existing = new EmailVerificationToken();
        existing.setStatus(VerificationStatus.ACTIVE);
        when(tokenRepository.findByEmailAndStatus(EMAIL, VerificationStatus.ACTIVE)).thenReturn(Optional.of(existing));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createToken(EMAIL, USER_ID);

        assertThat(existing.getStatus()).isEqualTo(VerificationStatus.EXPIRED);
        verify(tokenRepository, times(2)).save(any(EmailVerificationToken.class));
    }

    @Test
    @DisplayName("verify - valid token - calls Feign and marks VERIFIED")
    void verify_validToken_callsFeignAndMarksVerified() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken("valid-token");
        token.setStatus(VerificationStatus.ACTIVE);
        token.setExpiresAt(LocalDateTime.now().plusHours(24));
        token.setUserId(USER_ID);

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.verify("valid-token");

        verify(userServiceClient).verifyEmail(USER_ID.toString());
        assertThat(token.getStatus()).isEqualTo(VerificationStatus.VERIFIED);
    }

    @Test
    @DisplayName("verify - invalid token - throws ResourceNotFoundException")
    void verify_invalidToken_throwsResourceNotFound() {
        when(tokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify("bad-token"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("verify - already verified token - throws BusinessRuleException")
    void verify_alreadyVerifiedToken_throwsBusinessRule() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setStatus(VerificationStatus.VERIFIED);
        when(tokenRepository.findByToken("verified-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verify("verified-token"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    @DisplayName("verify - expired token - marks EXPIRED and throws")
    void verify_expiredToken_marksExpiredAndThrows() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setStatus(VerificationStatus.ACTIVE);
        token.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.verify("expired-token"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("expired");

        assertThat(token.getStatus()).isEqualTo(VerificationStatus.EXPIRED);
        verify(tokenRepository).save(token);
    }
}

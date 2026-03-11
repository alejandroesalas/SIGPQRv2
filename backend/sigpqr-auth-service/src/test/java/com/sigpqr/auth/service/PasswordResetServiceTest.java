package com.sigpqr.auth.service;

import com.sigpqr.auth.client.UserServiceClient;
import com.sigpqr.auth.dto.UserCredentialsDto;
import com.sigpqr.auth.entity.PasswordResetToken;
import com.sigpqr.auth.enums.TokenStatus;
import com.sigpqr.auth.repository.PasswordResetTokenRepository;
import com.sigpqr.common.constants.RabbitMQConstants;
import com.sigpqr.common.exception.BusinessRuleException;
import com.sigpqr.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService service;

    private static final String EMAIL = "user@example.com";
    private static final UUID USER_ID = UUID.randomUUID();

    private UserCredentialsDto validUser() {
        return new UserCredentialsDto(USER_ID, EMAIL, "hashedPw", "STUDENT", true, true);
    }

    @Test
    @DisplayName("requestReset - valid email - saves token and publishes event")
    void requestReset_validEmail_savesTokenAndPublishesEvent() {
        when(userServiceClient.findByEmail(EMAIL)).thenReturn(validUser());
        when(tokenRepository.findByEmailAndStatus(EMAIL, TokenStatus.ACTIVE)).thenReturn(Optional.empty());
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> inv.getArgument(0));

        service.requestReset(EMAIL);

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        PasswordResetToken saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getStatus()).isEqualTo(TokenStatus.ACTIVE);
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConstants.AUTH_EVENTS_EXCHANGE),
                eq(RabbitMQConstants.AUTH_PASSWORD_RESET_KEY),
                any(Object.class)
        );
    }

    @Test
    @DisplayName("requestReset - unknown email - throws ResourceNotFoundException")
    void requestReset_unknownEmail_throwsResourceNotFound() {
        when(userServiceClient.findByEmail(EMAIL)).thenReturn(null);

        assertThatThrownBy(() -> service.requestReset(EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("requestReset - existing active token - invalidates previous")
    void requestReset_existingActiveToken_invalidatesPrevious() {
        when(userServiceClient.findByEmail(EMAIL)).thenReturn(validUser());
        PasswordResetToken existing = new PasswordResetToken();
        existing.setStatus(TokenStatus.ACTIVE);
        when(tokenRepository.findByEmailAndStatus(EMAIL, TokenStatus.ACTIVE)).thenReturn(Optional.of(existing));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> inv.getArgument(0));

        service.requestReset(EMAIL);

        assertThat(existing.getStatus()).isEqualTo(TokenStatus.EXPIRED);
        verify(tokenRepository, times(2)).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("resetPassword - valid token - updates password and marks USED")
    void resetPassword_validToken_updatesPasswordAndMarksUsed() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("valid-token");
        token.setStatus(TokenStatus.ACTIVE);
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setUserId(USER_ID);

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPass123")).thenReturn("encodedPass");
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.resetPassword("valid-token", "NewPass123");

        verify(passwordEncoder).encode("NewPass123");
        verify(userServiceClient).updatePassword(USER_ID.toString(), "encodedPass");
        assertThat(token.getStatus()).isEqualTo(TokenStatus.USED);
    }

    @Test
    @DisplayName("resetPassword - invalid token - throws ResourceNotFoundException")
    void resetPassword_invalidToken_throwsResourceNotFound() {
        when(tokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("bad-token", "pass"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("resetPassword - already used token - throws BusinessRuleException")
    void resetPassword_alreadyUsedToken_throwsBusinessRule() {
        PasswordResetToken token = new PasswordResetToken();
        token.setStatus(TokenStatus.USED);
        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword("used-token", "pass"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    @DisplayName("resetPassword - expired token - marks EXPIRED and throws")
    void resetPassword_expiredToken_marksExpiredAndThrows() {
        PasswordResetToken token = new PasswordResetToken();
        token.setStatus(TokenStatus.ACTIVE);
        token.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.resetPassword("expired-token", "pass"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("expired");

        assertThat(token.getStatus()).isEqualTo(TokenStatus.EXPIRED);
        verify(tokenRepository).save(token);
    }
}

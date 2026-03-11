package com.sigpqr.auth.service;

import com.sigpqr.auth.client.UserServiceClient;
import com.sigpqr.auth.dto.UserCredentialsDto;
import com.sigpqr.auth.entity.PasswordResetToken;
import com.sigpqr.auth.enums.TokenStatus;
import com.sigpqr.auth.event.PasswordResetEvent;
import com.sigpqr.auth.repository.PasswordResetTokenRepository;
import com.sigpqr.common.constants.AppConstants;
import com.sigpqr.common.constants.RabbitMQConstants;
import com.sigpqr.common.exception.BusinessRuleException;
import com.sigpqr.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final PasswordResetTokenRepository tokenRepository;
    private final UserServiceClient userServiceClient;
    private final RabbitTemplate rabbitTemplate;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserServiceClient userServiceClient,
                                RabbitTemplate rabbitTemplate,
                                PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userServiceClient = userServiceClient;
        this.rabbitTemplate = rabbitTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void requestReset(String email) {
        String cid = AppConstants.getRequestId();
        log.info("[correlationId={}] Password reset requested for email={}", cid, email);

        UserCredentialsDto user;
        try {
            user = userServiceClient.findByEmail(email);
        } catch (Exception e) {
            log.error("[correlationId={}] Feign call to user-service failed for email={}: {}",
                    cid, email, e.getMessage(), e);
            throw new ResourceNotFoundException("User", "email", email);
        }

        if (user == null) {
            log.warn("[correlationId={}] User not found for password reset: email={}", cid, email);
            throw new ResourceNotFoundException("User", "email", email);
        }

        // Invalidate any existing active tokens for this email
        tokenRepository.findByEmailAndStatus(email, TokenStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.setStatus(TokenStatus.EXPIRED);
                    tokenRepository.save(existing);
                });

        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setEmail(email);
        token.setUserId(user.userId());
        token.setStatus(TokenStatus.ACTIVE);
        token.setExpiresAt(LocalDateTime.now().plusHours(1));

        tokenRepository.save(token);

        PasswordResetEvent event = new PasswordResetEvent(
                email,
                token.getToken(),
                token.getExpiresAt()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConstants.AUTH_EVENTS_EXCHANGE,
                RabbitMQConstants.AUTH_PASSWORD_RESET_KEY,
                event
        );

        log.info("[correlationId={}] Password reset event published for email={}", cid, email);
    }

    @Transactional
    public void resetPassword(String tokenValue, String newPassword) {
        String cid = AppConstants.getRequestId();
        log.info("[correlationId={}] Password reset attempt with token", cid);

        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> {
                    log.warn("[correlationId={}] Reset token not found", cid);
                    return new ResourceNotFoundException("PasswordResetToken", "token", tokenValue);
                });

        if (token.getStatus() != TokenStatus.ACTIVE) {
            log.warn("[correlationId={}] Reset token already used/expired, status={}", cid, token.getStatus());
            throw new BusinessRuleException("Reset token has already been used or expired");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            token.setStatus(TokenStatus.EXPIRED);
            tokenRepository.save(token);
            log.warn("[correlationId={}] Reset token expired at {}", cid, token.getExpiresAt());
            throw new BusinessRuleException("Reset token has expired");
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        try {
            userServiceClient.updatePassword(token.getUserId().toString(), encodedPassword);
        } catch (Exception e) {
            log.error("[correlationId={}] Feign call to update password failed for userId={}: {}",
                    cid, token.getUserId(), e.getMessage(), e);
            throw e;
        }

        token.setStatus(TokenStatus.USED);
        tokenRepository.save(token);
        log.info("[correlationId={}] Password reset completed for userId={}", cid, token.getUserId());
    }
}

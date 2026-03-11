package com.sigpqr.auth.service;

import com.sigpqr.auth.client.UserServiceClient;
import com.sigpqr.auth.entity.EmailVerificationToken;
import com.sigpqr.auth.enums.VerificationStatus;
import com.sigpqr.auth.repository.EmailVerificationTokenRepository;
import com.sigpqr.common.constants.AppConstants;
import com.sigpqr.common.exception.BusinessRuleException;
import com.sigpqr.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserServiceClient userServiceClient;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
                                     UserServiceClient userServiceClient) {
        this.tokenRepository = tokenRepository;
        this.userServiceClient = userServiceClient;
    }

    @Transactional
    public String createToken(String email, UUID userId) {
        String cid = MDC.get(AppConstants.CORRELATION_ID_MDC_KEY);
        log.info("[correlationId={}] Creating email verification token for email={}, userId={}",
                cid, email, userId);

        tokenRepository.findByEmailAndStatus(email, VerificationStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.setStatus(VerificationStatus.EXPIRED);
                    tokenRepository.save(existing);
                    log.info("[correlationId={}] Invalidated existing verification token for email={}", cid, email);
                });

        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setEmail(email);
        token.setUserId(userId);
        token.setStatus(VerificationStatus.ACTIVE);
        token.setExpiresAt(LocalDateTime.now().plusHours(24));

        tokenRepository.save(token);
        log.info("[correlationId={}] Verification token created for email={}", cid, email);
        return token.getToken();
    }

    @Transactional
    public void verify(String tokenValue) {
        String cid = MDC.get(AppConstants.CORRELATION_ID_MDC_KEY);
        log.info("[correlationId={}] Email verification attempt", cid);

        EmailVerificationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> {
                    log.warn("[correlationId={}] Verification token not found", cid);
                    return new ResourceNotFoundException("EmailVerificationToken", "token", tokenValue);
                });

        if (token.getStatus() != VerificationStatus.ACTIVE) {
            log.warn("[correlationId={}] Verification token already used/expired, status={}",
                    cid, token.getStatus());
            throw new BusinessRuleException("Verification token has already been used or expired");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            token.setStatus(VerificationStatus.EXPIRED);
            tokenRepository.save(token);
            log.warn("[correlationId={}] Verification token expired at {}", cid, token.getExpiresAt());
            throw new BusinessRuleException("Verification token has expired");
        }

        try {
            userServiceClient.verifyEmail(token.getUserId().toString());
        } catch (Exception e) {
            log.error("[correlationId={}] Feign call to verify email failed for userId={}: {}",
                    cid, token.getUserId(), e.getMessage(), e);
            throw e;
        }

        token.setStatus(VerificationStatus.VERIFIED);
        tokenRepository.save(token);
        log.info("[correlationId={}] Email verified for userId={}", cid, token.getUserId());
    }
}

package com.sigpqr.auth.service;

import com.sigpqr.auth.client.UserServiceClient;
import com.sigpqr.auth.entity.EmailVerificationToken;
import com.sigpqr.auth.enums.VerificationStatus;
import com.sigpqr.auth.repository.EmailVerificationTokenRepository;
import com.sigpqr.common.exception.BusinessRuleException;
import com.sigpqr.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserServiceClient userServiceClient;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
                                     UserServiceClient userServiceClient) {
        this.tokenRepository = tokenRepository;
        this.userServiceClient = userServiceClient;
    }

    @Transactional
    public String createToken(String email, UUID userId) {
        // Invalidate any existing active tokens for this email
        tokenRepository.findByEmailAndStatus(email, VerificationStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.setStatus(VerificationStatus.EXPIRED);
                    tokenRepository.save(existing);
                });

        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setEmail(email);
        token.setUserId(userId);
        token.setStatus(VerificationStatus.ACTIVE);
        token.setExpiresAt(LocalDateTime.now().plusHours(24));

        tokenRepository.save(token);
        return token.getToken();
    }

    @Transactional
    public void verify(String tokenValue) {
        EmailVerificationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ResourceNotFoundException("EmailVerificationToken", "token", tokenValue));

        if (token.getStatus() != VerificationStatus.ACTIVE) {
            throw new BusinessRuleException("Verification token has already been used or expired");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            token.setStatus(VerificationStatus.EXPIRED);
            tokenRepository.save(token);
            throw new BusinessRuleException("Verification token has expired");
        }

        userServiceClient.verifyEmail(token.getUserId().toString());

        token.setStatus(VerificationStatus.VERIFIED);
        tokenRepository.save(token);
    }
}

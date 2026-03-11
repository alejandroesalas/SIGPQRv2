package com.sigpqr.user.service;

import com.sigpqr.common.constants.AppConstants;
import com.sigpqr.common.exception.ResourceNotFoundException;
import com.sigpqr.user.dto.UserCredentialsDto;
import com.sigpqr.user.entity.UserEntity;
import com.sigpqr.user.enums.UserStatus;
import com.sigpqr.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class InternalUserService {

    private static final Logger log = LoggerFactory.getLogger(InternalUserService.class);

    private final UserRepository userRepository;

    public InternalUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserCredentialsDto findByEmail(String email) {
        String cid = AppConstants.getRequestId();
        log.info("[correlationId={}] Looking up credentials for email={}", cid, email);

        UserEntity user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> {
                    log.warn("[correlationId={}] User not found for email={}", cid, email);
                    return new ResourceNotFoundException("User", "email", email);
                });

        log.info("[correlationId={}] Credentials found for email={}, profile={}, status={}, verified={}",
                cid, email, user.getProfile().name(),
                user.getStatus(), user.isVerified());

        return new UserCredentialsDto(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getProfile().name(),
                user.getStatus() == UserStatus.ACTIVE,
                user.isVerified()
        );
    }

    public void updatePassword(UUID userId, String passwordHash) {
        String cid = AppConstants.getRequestId();
        log.info("[correlationId={}] Updating password for userId={}", cid, userId);

        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> {
                    log.warn("[correlationId={}] User not found for password update, userId={}", cid, userId);
                    return new ResourceNotFoundException("User", "id", userId);
                });

        user.setPasswordHash(passwordHash);
        userRepository.save(user);
        log.info("[correlationId={}] Password updated for userId={}", cid, userId);
    }

    public void verifyEmail(UUID userId) {
        String cid = AppConstants.getRequestId();
        log.info("[correlationId={}] Verifying email for userId={}", cid, userId);

        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> {
                    log.warn("[correlationId={}] User not found for email verification, userId={}", cid, userId);
                    return new ResourceNotFoundException("User", "id", userId);
                });

        user.setVerified(true);
        userRepository.save(user);
        log.info("[correlationId={}] Email verified for userId={}", cid, userId);
    }
}

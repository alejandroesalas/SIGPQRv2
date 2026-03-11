package com.sigpqr.auth.repository;

import com.sigpqr.auth.entity.EmailVerificationToken;
import com.sigpqr.auth.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findByEmailAndStatus(String email, VerificationStatus status);
}

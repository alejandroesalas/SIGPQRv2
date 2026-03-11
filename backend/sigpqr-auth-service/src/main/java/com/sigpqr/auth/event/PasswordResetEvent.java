package com.sigpqr.auth.event;

import java.time.LocalDateTime;

public record PasswordResetEvent(
        String email,
        String resetToken,
        LocalDateTime expiresAt
) {}

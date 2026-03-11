package com.sigpqr.user.event;

import java.util.UUID;

public record UserRegisteredEvent(
        UUID userId,
        String email,
        String verificationToken
) {}

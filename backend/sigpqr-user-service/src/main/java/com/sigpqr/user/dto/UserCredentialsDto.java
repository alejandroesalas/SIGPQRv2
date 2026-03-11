package com.sigpqr.user.dto;

import java.util.UUID;

public record UserCredentialsDto(
        UUID userId,
        String email,
        String passwordHash,
        String profileId,
        boolean enabled,
        boolean verified
) {}

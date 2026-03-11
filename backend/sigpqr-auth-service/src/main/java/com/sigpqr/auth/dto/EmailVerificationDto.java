package com.sigpqr.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailVerificationDto(
        @NotBlank(message = "Token is required")
        String token
) {}

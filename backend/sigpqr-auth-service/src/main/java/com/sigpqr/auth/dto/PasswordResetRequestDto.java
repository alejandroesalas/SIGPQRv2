package com.sigpqr.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequestDto(
        @NotBlank(message = "Email is required")
        String email
) {}

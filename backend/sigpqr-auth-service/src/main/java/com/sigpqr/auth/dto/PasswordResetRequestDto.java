package com.sigpqr.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload to initiate a password reset")
public record PasswordResetRequestDto(
        @Schema(description = "Email address of the account", example = "user@example.com")
        @NotBlank(message = "Email is required")
        String email
) {}

package com.sigpqr.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to reset a password using a token")
public record PasswordResetDto(
        @Schema(description = "Password reset token received via email")
        @NotBlank(message = "Token is required")
        String token,

        @Schema(description = "New password (minimum 8 characters)", example = "newSecurePass123")
        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword
) {}

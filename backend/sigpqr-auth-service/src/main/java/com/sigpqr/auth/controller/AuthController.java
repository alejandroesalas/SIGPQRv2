package com.sigpqr.auth.controller;

import com.sigpqr.auth.dto.PasswordResetDto;
import com.sigpqr.auth.dto.PasswordResetRequestDto;
import com.sigpqr.auth.service.PasswordResetService;
import com.sigpqr.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Password reset endpoints")
public class AuthController {

    private final PasswordResetService passwordResetService;

    public AuthController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @Operation(summary = "Request password reset", description = "Sends a password reset email to the specified address")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password reset email sent")
    @PostMapping("/password/reset-request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDto request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok(ApiResponse.ok("Password reset email sent", null));
    }

    @Operation(summary = "Reset password", description = "Resets the password using a valid token and new password")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password has been reset successfully")
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetDto request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password has been reset successfully", null));
    }
}

package com.sigpqr.user.controller;

import com.sigpqr.user.dto.UserCredentialsDto;
import com.sigpqr.user.service.InternalUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/internal")
@Tag(name = "Internal", description = "Service-to-service endpoints (called by auth-service)")
public class InternalUserController {

    private final InternalUserService internalUserService;

    public InternalUserController(InternalUserService internalUserService) {
        this.internalUserService = internalUserService;
    }

    @GetMapping("/by-email")
    @Operation(summary = "Find user by email", description = "Lookup user credentials by email (used by auth-service)")
    public UserCredentialsDto findByEmail(@RequestParam String email) {
        return internalUserService.findByEmail(email);
    }

    @GetMapping("/update-password")
    @Operation(summary = "Update password", description = "Update user password hash (used by auth-service)")
    public void updatePassword(@RequestParam String userId, @RequestParam String passwordHash) {
        internalUserService.updatePassword(UUID.fromString(userId), passwordHash);
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Mark user email as verified (used by auth-service)")
    public void verifyEmail(@RequestParam String userId) {
        internalUserService.verifyEmail(UUID.fromString(userId));
    }
}

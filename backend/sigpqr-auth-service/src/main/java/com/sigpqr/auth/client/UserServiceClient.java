package com.sigpqr.auth.client;

import com.sigpqr.auth.dto.UserCredentialsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "sigpqr-user-service")
public interface UserServiceClient {

    @GetMapping("/api/users/internal/by-email")
    UserCredentialsDto findByEmail(@RequestParam("email") String email);

    @GetMapping("/api/users/internal/update-password")
    void updatePassword(@RequestParam("userId") String userId,
                        @RequestParam("passwordHash") String passwordHash);

    @GetMapping("/api/users/internal/verify-email")
    void verifyEmail(@RequestParam("userId") String userId);
}

package com.sigpqr.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigpqr.auth.dto.PasswordResetDto;
import com.sigpqr.auth.dto.PasswordResetRequestDto;
import com.sigpqr.auth.service.PasswordResetService;
import com.sigpqr.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PasswordResetService passwordResetService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("resetRequest - valid email - returns 200")
    void resetRequest_validEmail_returns200() throws Exception {
        doNothing().when(passwordResetService).requestReset("user@example.com");

        mockMvc.perform(post("/api/auth/password/reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetRequestDto("user@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset email sent"));

        verify(passwordResetService).requestReset("user@example.com");
    }

    @Test
    @DisplayName("resetRequest - blank email - returns 400")
    void resetRequest_blankEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/password/reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetRequestDto(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("reset - valid payload - returns 200")
    void reset_validPayload_returns200() throws Exception {
        doNothing().when(passwordResetService).resetPassword("token123", "NewPassword1");

        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetDto("token123", "NewPassword1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password has been reset successfully"));

        verify(passwordResetService).resetPassword("token123", "NewPassword1");
    }

    @Test
    @DisplayName("reset - short password - returns 400")
    void reset_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetDto("token123", "short"))))
                .andExpect(status().isBadRequest());
    }
}

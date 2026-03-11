package com.sigpqr.user.dto;

import com.sigpqr.user.enums.IdType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "User registration request")
public record CreateUserDto(
        @Schema(description = "First name")
        @NotBlank(message = "Name is required")
        String name,

        @Schema(description = "Last name")
        @NotBlank(message = "Last name is required")
        String lastname,

        @Schema(description = "Email address")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @Schema(description = "Password (min 8 characters)")
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @Schema(description = "Identification type")
        @NotNull(message = "ID type is required")
        IdType idType,

        @Schema(description = "Identification number")
        @NotBlank(message = "ID number is required")
        String idNumber,

        @Schema(description = "Profile ID (1=Admin, 2=Coordinator, 3=Student, 4=Teacher)")
        @NotNull(message = "Profile ID is required")
        Long profileId,

        @Schema(description = "Academic program ID (nullable)")
        Long programId
) {}

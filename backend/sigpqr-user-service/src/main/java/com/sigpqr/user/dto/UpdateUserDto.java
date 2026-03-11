package com.sigpqr.user.dto;

import com.sigpqr.user.enums.IdType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "User update request")
public record UpdateUserDto(
        @Schema(description = "First name")
        @NotBlank(message = "Name is required")
        String name,

        @Schema(description = "Last name")
        @NotBlank(message = "Last name is required")
        String lastname,

        @Schema(description = "Identification type")
        @NotNull(message = "ID type is required")
        IdType idType,

        @Schema(description = "Identification number")
        @NotBlank(message = "ID number is required")
        String idNumber,

        @Schema(description = "Academic program ID (nullable)")
        Long programId
) {}

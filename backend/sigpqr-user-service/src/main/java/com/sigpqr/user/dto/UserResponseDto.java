package com.sigpqr.user.dto;

import com.sigpqr.user.entity.UserEntity;
import com.sigpqr.user.enums.IdType;
import com.sigpqr.user.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "User response")
public record UserResponseDto(
        UUID id,
        String name,
        String lastname,
        String email,
        IdType idType,
        String idNumber,
        boolean verified,
        UserStatus status,
        Long profileId,
        Long programId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static UserResponseDto from(UserEntity entity) {
        return new UserResponseDto(
                entity.getId(),
                entity.getName(),
                entity.getLastname(),
                entity.getEmail(),
                entity.getIdType(),
                entity.getIdNumber(),
                entity.isVerified(),
                entity.getStatus(),
                entity.getProfileId(),
                entity.getProgramId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

package com.sigpqr.user.dto;

import com.sigpqr.user.entity.ProfileEntity;

public record ProfileResponseDto(
        Long id,
        String name,
        String description
) {

    public static ProfileResponseDto from(ProfileEntity entity) {
        return new ProfileResponseDto(entity.getId(), entity.getName(), entity.getDescription());
    }
}

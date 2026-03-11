package com.sigpqr.user.converter;

import com.sigpqr.common.enums.Profile;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ProfileConverter implements AttributeConverter<Profile, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Profile profile) {
        return profile == null ? null : profile.getId();
    }

    @Override
    public Profile convertToEntityAttribute(Integer id) {
        return id == null ? null : Profile.fromId(id);
    }
}

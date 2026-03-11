package com.sigpqr.user.service;

import com.sigpqr.user.dto.ProfileResponseDto;
import com.sigpqr.user.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public List<ProfileResponseDto> listProfiles() {
        return profileRepository.findByDeletedFalse().stream()
                .map(ProfileResponseDto::from)
                .toList();
    }
}

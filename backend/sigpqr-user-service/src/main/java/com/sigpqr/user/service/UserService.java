package com.sigpqr.user.service;

import com.sigpqr.common.constants.AppConstants;
import com.sigpqr.common.constants.RabbitMQConstants;
import com.sigpqr.common.enums.Profile;
import com.sigpqr.common.exception.BusinessRuleException;
import com.sigpqr.common.exception.ResourceAlreadyExistsException;
import com.sigpqr.common.exception.ResourceNotFoundException;
import com.sigpqr.user.dto.CreateUserDto;
import com.sigpqr.user.dto.UpdateUserDto;
import com.sigpqr.user.dto.UserCountDto;
import com.sigpqr.user.dto.UserResponseDto;
import com.sigpqr.user.entity.ProfileEntity;
import com.sigpqr.user.entity.UserEntity;
import com.sigpqr.user.event.UserRegisteredEvent;
import com.sigpqr.user.repository.ProfileRepository;
import com.sigpqr.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;

    public UserService(UserRepository userRepository,
                       ProfileRepository profileRepository,
                       PasswordEncoder passwordEncoder,
                       RabbitTemplate rabbitTemplate) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDto> listUsers(Long profileId, Pageable pageable) {
        Page<UserEntity> page;
        if (profileId != null) {
            page = userRepository.findByProfileIdAndDeletedFalse(profileId, pageable);
        } else {
            page = userRepository.findAllByDeletedFalse(pageable);
        }
        return page.map(UserResponseDto::from);
    }

    public UserResponseDto createUser(CreateUserDto dto) {
        String cid = AppConstants.getRequestId();
        log.info("[correlationId={}] Creating user with email={}", cid, dto.email());

        if (userRepository.existsByEmailAndDeletedFalse(dto.email())) {
            log.warn("[correlationId={}] User already exists with email={}", cid, dto.email());
            throw new ResourceAlreadyExistsException("User", "email", dto.email());
        }

        UserEntity user = new UserEntity();
        user.setName(dto.name());
        user.setLastname(dto.lastname());
        user.setEmail(dto.email());
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setIdType(dto.idType());
        user.setIdNumber(dto.idNumber());
        user.setProfileId(dto.profileId());
        user.setProgramId(dto.programId());

        user = userRepository.save(user);
        log.info("[correlationId={}] User created: id={}, email={}", cid, user.getId(), user.getEmail());

        String verificationToken = UUID.randomUUID().toString();
        var event = new UserRegisteredEvent(user.getId(), user.getEmail(), verificationToken);
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.USER_EVENTS_EXCHANGE,
                RabbitMQConstants.USER_REGISTERED_KEY,
                event
        );
        log.info("[correlationId={}] User registered event published for userId={}", cid, user.getId());

        return UserResponseDto.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserById(UUID id) {
        UserEntity user = findActiveUserOrThrow(id);
        return UserResponseDto.from(user);
    }

    public UserResponseDto updateUser(UUID id, UpdateUserDto dto) {
        UserEntity user = findActiveUserOrThrow(id);
        user.setName(dto.name());
        user.setLastname(dto.lastname());
        user.setIdType(dto.idType());
        user.setIdNumber(dto.idNumber());
        user.setProgramId(dto.programId());
        user = userRepository.save(user);
        return UserResponseDto.from(user);
    }

    public void deleteUser(UUID id) {
        String cid = AppConstants.getRequestId();
        log.info("[correlationId={}] Deleting user id={}", cid, id);
        UserEntity user = findActiveUserOrThrow(id);
        user.setDeleted(true);
        userRepository.save(user);
        log.info("[correlationId={}] User soft-deleted: id={}", cid, id);
    }

    public UserResponseDto promoteToCoordinator(UUID id) {
        String cid = AppConstants.getRequestId();
        log.info("[correlationId={}] Promoting user id={} to coordinator", cid, id);
        UserEntity user = findActiveUserOrThrow(id);
        if (!user.getProfileId().equals((long) Profile.TEACHER.getId())) {
            log.warn("[correlationId={}] Cannot promote user id={}, current profile is not TEACHER", cid, id);
            throw new BusinessRuleException("Only teachers can be promoted to coordinator");
        }
        user.setProfileId((long) Profile.COORDINATOR.getId());
        user = userRepository.save(user);
        log.info("[correlationId={}] User promoted to coordinator: id={}", cid, id);
        return UserResponseDto.from(user);
    }

    public UserResponseDto demoteToTeacher(UUID id) {
        String cid = AppConstants.getRequestId();
        log.info("[correlationId={}] Demoting user id={} to teacher", cid, id);
        UserEntity user = findActiveUserOrThrow(id);
        if (!user.getProfileId().equals((long) Profile.COORDINATOR.getId())) {
            log.warn("[correlationId={}] Cannot demote user id={}, current profile is not COORDINATOR", cid, id);
            throw new BusinessRuleException("Only coordinators can be demoted to teacher");
        }
        user.setProfileId((long) Profile.TEACHER.getId());
        user = userRepository.save(user);
        log.info("[correlationId={}] User demoted to teacher: id={}", cid, id);
        return UserResponseDto.from(user);
    }

    public UserResponseDto restoreUser(UUID id) {
        String cid = AppConstants.getRequestId();
        log.info("[correlationId={}] Restoring user id={}", cid, id);
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[correlationId={}] User not found for restore: id={}", cid, id);
                    return new ResourceNotFoundException("User", "id", id);
                });
        if (!user.isDeleted()) {
            log.warn("[correlationId={}] User id={} is not deleted, cannot restore", cid, id);
            throw new BusinessRuleException("User is not deleted");
        }
        user.setDeleted(false);
        user = userRepository.save(user);
        log.info("[correlationId={}] User restored: id={}", cid, id);
        return UserResponseDto.from(user);
    }

    @Transactional(readOnly = true)
    public List<UserCountDto> countByProfile() {
        List<ProfileEntity> profiles = profileRepository.findByDeletedFalse();
        return profiles.stream()
                .map(p -> new UserCountDto(
                        p.getName(),
                        userRepository.countByProfileIdAndDeletedFalse(p.getId())
                ))
                .toList();
    }

    private UserEntity findActiveUserOrThrow(UUID id) {
        return userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
}

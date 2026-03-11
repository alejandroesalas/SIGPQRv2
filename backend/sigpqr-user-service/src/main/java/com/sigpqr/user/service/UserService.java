package com.sigpqr.user.service;

import com.sigpqr.common.constants.AppConstants;
import com.sigpqr.common.constants.RabbitMQConstants;
import com.sigpqr.common.enums.Profile;
import com.sigpqr.common.exception.BusinessRuleException;
import com.sigpqr.common.exception.ResourceAlreadyExistsException;
import com.sigpqr.common.exception.ResourceNotFoundException;
import com.sigpqr.user.dto.RegisterStudentDto;
import com.sigpqr.user.dto.RegisterTeacherDto;
import com.sigpqr.user.dto.UpdateUserDto;
import com.sigpqr.user.dto.UserCountDto;
import com.sigpqr.user.dto.UserResponseDto;
import com.sigpqr.user.entity.UserEntity;
import com.sigpqr.user.enums.IdType;
import com.sigpqr.user.event.UserRegisteredEvent;
import com.sigpqr.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       RabbitTemplate rabbitTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDto> listUsers(Profile profile, Pageable pageable) {
        Page<UserEntity> page;
        if (profile != null) {
            page = userRepository.findByProfileAndDeletedFalse(profile, pageable);
        } else {
            page = userRepository.findAllByDeletedFalse(pageable);
        }
        return page.map(UserResponseDto::from);
    }

    public UserResponseDto registerStudent(RegisterStudentDto dto) {
        return createUserInternal(
                dto.name(), dto.lastname(), dto.email(), dto.password(),
                dto.idType(), dto.idNumber(), Profile.STUDENT, dto.programId()
        );
    }

    public UserResponseDto createTeacher(RegisterTeacherDto dto) {
        return createUserInternal(
                dto.name(), dto.lastname(), dto.email(), dto.password(),
                dto.idType(), dto.idNumber(), Profile.TEACHER, dto.programId()
        );
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
        if (user.getProfile() != Profile.TEACHER) {
            log.warn("[correlationId={}] Cannot promote user id={}, current profile is not TEACHER", cid, id);
            throw new BusinessRuleException("Only teachers can be promoted to coordinator");
        }
        user.setProfile(Profile.COORDINATOR);
        user = userRepository.save(user);
        log.info("[correlationId={}] User promoted to coordinator: id={}", cid, id);
        return UserResponseDto.from(user);
    }

    public UserResponseDto demoteToTeacher(UUID id) {
        String cid = AppConstants.getRequestId();
        log.info("[correlationId={}] Demoting user id={} to teacher", cid, id);
        UserEntity user = findActiveUserOrThrow(id);
        if (user.getProfile() != Profile.COORDINATOR) {
            log.warn("[correlationId={}] Cannot demote user id={}, current profile is not COORDINATOR", cid, id);
            throw new BusinessRuleException("Only coordinators can be demoted to teacher");
        }
        user.setProfile(Profile.TEACHER);
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
        return Arrays.stream(Profile.values())
                .map(p -> new UserCountDto(
                        p.name(),
                        userRepository.countByProfileAndDeletedFalse(p)
                ))
                .toList();
    }

    private UserResponseDto createUserInternal(String name, String lastname, String email,
                                                String password, IdType idType, String idNumber,
                                                Profile profile, Long programId) {
        String cid = AppConstants.getRequestId();
        log.info("[correlationId={}] Creating user with email={}", cid, email);

        if (userRepository.existsByEmailAndDeletedFalse(email)) {
            log.warn("[correlationId={}] User already exists with email={}", cid, email);
            throw new ResourceAlreadyExistsException("User", "email", email);
        }

        UserEntity user = new UserEntity();
        user.setName(name);
        user.setLastname(lastname);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setIdType(idType);
        user.setIdNumber(idNumber);
        user.setProfile(profile);
        user.setProgramId(programId);

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

    private UserEntity findActiveUserOrThrow(UUID id) {
        return userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
}

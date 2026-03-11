package com.sigpqr.user.repository;

import com.sigpqr.common.enums.Profile;
import com.sigpqr.user.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByIdAndDeletedFalse(UUID id);

    Optional<UserEntity> findByEmailAndDeletedFalse(String email);

    Page<UserEntity> findByProfileAndDeletedFalse(Profile profile, Pageable pageable);

    Page<UserEntity> findAllByDeletedFalse(Pageable pageable);

    boolean existsByEmailAndDeletedFalse(String email);

    long countByProfileAndDeletedFalse(Profile profile);
}

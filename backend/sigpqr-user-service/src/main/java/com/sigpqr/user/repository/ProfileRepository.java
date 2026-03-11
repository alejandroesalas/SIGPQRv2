package com.sigpqr.user.repository;

import com.sigpqr.user.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {

    List<ProfileEntity> findByDeletedFalse();
}

package com.sigpqr.user.config;

import com.sigpqr.user.entity.ProfileEntity;
import com.sigpqr.user.entity.UserEntity;
import com.sigpqr.user.enums.IdType;
import com.sigpqr.user.enums.UserStatus;
import com.sigpqr.user.repository.ProfileRepository;
import com.sigpqr.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(ProfileRepository profileRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedProfiles();
        seedUsers();
    }

    private void seedProfiles() {
        if (profileRepository.count() > 0) {
            log.info("Profiles already seeded, skipping.");
            return;
        }

        createProfile(1L, "Admin", "System administrator");
        createProfile(2L, "Coordinator", "Program coordinator");
        createProfile(3L, "Student", "Student user");
        createProfile(4L, "Teacher", "Teacher user");

        log.info("Seeded 4 profiles.");
    }

    private void createProfile(Long id, String name, String description) {
        ProfileEntity profile = new ProfileEntity();
        profile.setId(id);
        profile.setName(name);
        profile.setDescription(description);
        profileRepository.save(profile);
    }

    private void seedUsers() {
        String encodedPassword = passwordEncoder.encode("Password123");

        seedUser("Admin", "User", "admin@sigpqr.local", encodedPassword, "1000000001", 1L, null);
        seedUser("Carlos", "Estudiante", "carlos.estudiante@sigpqr.local", encodedPassword, "1000000002", 3L, 1L);
        seedUser("Maria", "Estudiante", "maria.estudiante@sigpqr.local", encodedPassword, "1000000003", 3L, 1L);
        seedUser("Pedro", "Docente", "pedro.docente@sigpqr.local", encodedPassword, "1000000004", 4L, 1L);
        seedUser("Ana", "Docente", "ana.docente@sigpqr.local", encodedPassword, "1000000005", 4L, 1L);
    }

    private void seedUser(String name, String lastname, String email,
                          String encodedPassword, String idNumber,
                          Long profileId, Long programId) {
        if (userRepository.existsByEmailAndDeletedFalse(email)) {
            log.info("User {} already exists, skipping.", email);
            return;
        }

        UserEntity user = new UserEntity();
        user.setName(name);
        user.setLastname(lastname);
        user.setEmail(email);
        user.setPasswordHash(encodedPassword);
        user.setIdType(IdType.CC);
        user.setIdNumber(idNumber);
        user.setVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setProfileId(profileId);
        user.setProgramId(programId);
        userRepository.save(user);

        log.info("Seeded user: {} (profile={})", email, profileId);
    }
}

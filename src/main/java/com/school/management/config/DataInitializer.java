package com.school.management.config;

import com.school.management.domain.user.Role;
import com.school.management.entity.UserEntity;
import com.school.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 1. Super Admin (Master Admin) - Only superadmin is initialized by default
        if (!userRepository.existsByRole(Role.MASTER_ADMIN) && !userRepository.existsByEmail("admin@school.com")) {
            userRepository.save(UserEntity.builder()
                    .email("admin@school.com")
                    .password(passwordEncoder.encode("admin123"))
                    .name("Super Admin")
                    .role(Role.MASTER_ADMIN)
                    .enabled(true)
                    .build());
        }

        // Cleanup any dummy / default test users so only superadmin remains
        userRepository.findByEmailIgnoreCase("teacher@schooldesk.com").ifPresent(userRepository::delete);
        userRepository.findByEmailIgnoreCase("student@schooldesk.com").ifPresent(userRepository::delete);
        userRepository.findByEmailIgnoreCase("parent@schooldesk.com").ifPresent(userRepository::delete);
        userRepository.findByEmailIgnoreCase("staff@schooldesk.com").ifPresent(userRepository::delete);
        userRepository.findByEmailIgnoreCase("adm99999@schooldesk.com").ifPresent(userRepository::delete);

        System.out.println("========================================");
        System.out.println("  SYSTEM INITIALIZED:");
        System.out.println("  Super Admin: admin@school.com / admin123");
        System.out.println("  (Teacher, Student, Parent, and Staff credentials");
        System.out.println("   are generated dynamically upon registration)");
        System.out.println("========================================");
    }
}


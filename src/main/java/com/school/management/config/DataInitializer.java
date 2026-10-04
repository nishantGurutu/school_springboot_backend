package com.school.management.config;

import com.school.management.domain.user.Role;
import com.school.management.entity.UserEntity;
import com.school.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Ensure default Super Admin account exists so admin can log into dashboard
        if (!userRepository.existsByRole(Role.MASTER_ADMIN) && !userRepository.existsByEmail("admin@school.com")) {
            userRepository.save(UserEntity.builder()
                    .email("admin@school.com")
                    .password(passwordEncoder.encode("admin123"))
                    .name("Super Admin")
                    .role(Role.MASTER_ADMIN)
                    .enabled(true)
                    .build());
        }

        System.out.println("========================================");
        System.out.println("  SYSTEM INITIALIZED:");
        System.out.println("  Super Admin : admin@school.com / admin123");
        System.out.println("  (All staff, teachers, students and school records are managed via Dashboard)");
        System.out.println("========================================");
    }
}

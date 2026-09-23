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
        // 1. Master Admin
        if (!userRepository.existsByRole(Role.MASTER_ADMIN)) {
            userRepository.save(UserEntity.builder()
                    .email("admin@school.com")
                    .password(passwordEncoder.encode("admin123"))
                    .name("System Admin")
                    .role(Role.MASTER_ADMIN)
                    .build());
        }

        System.out.println("========================================");
        System.out.println("  MASTER ADMIN READY: admin@school.com / admin123");
        System.out.println("========================================");
    }
}


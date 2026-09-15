package com.school.management.config;

import com.school.management.domain.user.Role;
import com.school.management.entity.UserEntity;
import com.school.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        boolean adminExists = userRepository.existsByRole(Role.MASTER_ADMIN);

        if (!adminExists) {
            UserEntity master = UserEntity.builder()
                    .email("admin@school.com")
                    .password(passwordEncoder.encode("admin123"))
                    .name("System Admin")
                    .role(Role.MASTER_ADMIN)
                    .build();

            userRepository.save(master);
            System.out.println("========================================");
            System.out.println("  MASTER ADMIN CREATED");
            System.out.println("  Email: admin@school.com");
            System.out.println("  Password: admin123");
            System.out.println("========================================");
        }
    }
}

package com.school.management.config;

import com.school.management.domain.user.Role;
import com.school.management.entity.StaffEntity;
import com.school.management.entity.UserEntity;
import com.school.management.repository.StaffRepository;
import com.school.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 1. Super Admin
        if (!userRepository.existsByRole(Role.MASTER_ADMIN) && !userRepository.existsByEmail("admin@school.com")) {
            userRepository.save(UserEntity.builder()
                    .email("admin@school.com")
                    .password(passwordEncoder.encode("admin123"))
                    .name("Super Admin")
                    .role(Role.MASTER_ADMIN)
                    .enabled(true)
                    .build());
        }

        // 2. Default Principal
        if (!userRepository.existsByEmail("principal@school.com")) {
            userRepository.save(UserEntity.builder()
                    .email("principal@school.com")
                    .password(passwordEncoder.encode("principal123"))
                    .name("Dr. Arvind Sharma (Principal)")
                    .role(Role.PRINCIPAL)
                    .enabled(true)
                    .build());

            if (!staffRepository.existsByEmail("principal@school.com")) {
                staffRepository.save(StaffEntity.builder()
                        .name("Dr. Arvind Sharma")
                        .staffType("Administration")
                        .designation("Principal")
                        .role("PRINCIPAL")
                        .email("principal@school.com")
                        .phone("9876543210")
                        .salary(95000.0)
                        .status("ACTIVE")
                        .joinDate(LocalDate.now().minusYears(3).toString())
                        .build());
            }
        }

        // 3. Default Accountant
        if (!userRepository.existsByEmail("accountant@school.com")) {
            userRepository.save(UserEntity.builder()
                    .email("accountant@school.com")
                    .password(passwordEncoder.encode("accountant123"))
                    .name("Rajesh Kumar (Accountant)")
                    .role(Role.ACCOUNTANT)
                    .enabled(true)
                    .build());

            if (!staffRepository.existsByEmail("accountant@school.com")) {
                staffRepository.save(StaffEntity.builder()
                        .name("Rajesh Kumar")
                        .staffType("Accounts")
                        .designation("Senior Accountant")
                        .role("ACCOUNTANT")
                        .email("accountant@school.com")
                        .phone("9876543211")
                        .salary(48000.0)
                        .status("ACTIVE")
                        .joinDate(LocalDate.now().minusYears(2).toString())
                        .build());
            }
        }

        // 4. Default Librarian
        if (!userRepository.existsByEmail("librarian@school.com")) {
            userRepository.save(UserEntity.builder()
                    .email("librarian@school.com")
                    .password(passwordEncoder.encode("librarian123"))
                    .name("Sunita Verma (Librarian)")
                    .role(Role.LIBRARIAN)
                    .enabled(true)
                    .build());

            if (!staffRepository.existsByEmail("librarian@school.com")) {
                staffRepository.save(StaffEntity.builder()
                        .name("Sunita Verma")
                        .staffType("Library")
                        .designation("Chief Librarian")
                        .role("LIBRARIAN")
                        .email("librarian@school.com")
                        .phone("9876543212")
                        .salary(42000.0)
                        .status("ACTIVE")
                        .joinDate(LocalDate.now().minusYears(1).toString())
                        .build());
            }
        }

        System.out.println("========================================");
        System.out.println("  SYSTEM INITIALIZED WITH RBAC ROLES:");
        System.out.println("  Super Admin : admin@school.com / admin123");
        System.out.println("  Principal   : principal@school.com / principal123");
        System.out.println("  Accountant  : accountant@school.com / accountant123");
        System.out.println("  Librarian   : librarian@school.com / librarian123");
        System.out.println("  (Teachers & custom staff can be added anytime via Dashboard)");
        System.out.println("========================================");
    }
}

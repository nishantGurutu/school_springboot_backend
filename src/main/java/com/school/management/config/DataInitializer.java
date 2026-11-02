package com.school.management.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.school.management.domain.user.Role;
import com.school.management.entity.UserEntity;
import com.school.management.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            // Update check constraint to support all enum roles including DRIVER
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
            jdbcTemplate.execute("ALTER TABLE users ADD CONSTRAINT users_role_check " +
                    "CHECK (role IN ('MASTER_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PRINCIPAL', 'TEACHER', 'STUDENT', 'PARENT', 'STAFF', 'ACCOUNTANT', 'LIBRARIAN', 'DRIVER'))");

            jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS department_id BIGINT");
            jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS designation_id BIGINT");
            jdbcTemplate.execute("ALTER TABLE school_designations ADD COLUMN IF NOT EXISTS department_id BIGINT");
            jdbcTemplate.execute("ALTER TABLE school_designations DROP CONSTRAINT IF EXISTS uk_desig_name");

            // Transport Schema Migrations
            jdbcTemplate.execute("ALTER TABLE buses ADD COLUMN IF NOT EXISTS permit_expiry_date DATE");
            jdbcTemplate.execute("ALTER TABLE transport_routes ADD COLUMN IF NOT EXISTS area_locality VARCHAR(128)");
            jdbcTemplate.execute("ALTER TABLE transport_routes ADD COLUMN IF NOT EXISTS fare DOUBLE PRECISION");
            jdbcTemplate.execute("ALTER TABLE driver_profiles ADD COLUMN IF NOT EXISTS joining_date DATE");
            jdbcTemplate.execute("ALTER TABLE student_transport_assignments ADD COLUMN IF NOT EXISTS trip_type VARCHAR(32)");
            jdbcTemplate.execute("ALTER TABLE student_transport_assignments ADD COLUMN IF NOT EXISTS start_date DATE");
            jdbcTemplate.execute("ALTER TABLE student_transport_assignments ADD COLUMN IF NOT EXISTS end_date DATE");
            jdbcTemplate.execute("ALTER TABLE student_transport_assignments ADD COLUMN IF NOT EXISTS monthly_fee DOUBLE PRECISION");
            jdbcTemplate.execute("ALTER TABLE student_transport_assignments ADD COLUMN IF NOT EXISTS fee_status VARCHAR(32)");
        } catch (Exception ignored) {
        }

        // MASTER_ADMIN has full access to the admin APIs in SecurityConfig.
        initializeAccount("admin@schooldesk.com", "schooldesk@1998", "Super Admin", Role.MASTER_ADMIN);
        initializeAccount("admin@school.com", "admin123", "Admin", Role.ADMIN);
    }

    private void initializeAccount(String email, String password, String name, Role role) {
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            return;
        }
        userRepository.save(UserEntity.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .role(role)
                .enabled(true)
                .build());
    }
}

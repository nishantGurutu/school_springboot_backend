package com.school.management.config;

import com.school.management.domain.user.Role;
import com.school.management.entity.UserEntity;
import com.school.management.repository.TeacherRepository;
import com.school.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
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

        // Purge any accidental teacher record with the admin email to prevent role
        // conflicts
        try {
            teacherRepository.findByEmailIgnoreCase("admin@school.com").ifPresent(teacherRepository::delete);
            jdbcTemplate.execute("DELETE FROM teachers WHERE LOWER(email) = 'admin@school.com'");
        } catch (Exception ignored) {
        }

        // Ensure default Super Admin account exists and its role/userType is ADMIN (not
        // TEACHER)
        Optional<UserEntity> existingAdmin = userRepository.findByEmailIgnoreCase("admin@school.com");
        if (existingAdmin.isPresent()) {
            UserEntity admin = existingAdmin.get();
            admin.setName("Super Admin");
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
        } else {
            userRepository.save(UserEntity.builder()
                    .email("admin@school.com")
                    .password(passwordEncoder.encode("admin123"))
                    .name("Super Admin")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build());
        }

        System.out.println("========================================");
        System.out.println("  SYSTEM INITIALIZED:");
        System.out.println("  Super Admin : admin@school.com / admin123 (Role: ADMIN)");
        System.out.println("  (All staff, teachers, students and school records are managed via Dashboard)");
        System.out.println("========================================");
    }
}

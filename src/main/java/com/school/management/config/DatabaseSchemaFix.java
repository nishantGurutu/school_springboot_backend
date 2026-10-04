package com.school.management.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.CommandLineRunner;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Fixes pre-existing database constraints and purges legacy code-seeded mock data.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaFix {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    public CommandLineRunner fixRoleCheckConstraint() {
        return args -> {
            String product;
            try (Connection conn = dataSource.getConnection()) {
                product = conn.getMetaData().getDatabaseProductName();
            } catch (Exception e) {
                log.warn("Could not determine database product name: {}", e.getMessage());
                return;
            }
            if (product == null || !product.toLowerCase().contains("postgres")) {
                return;
            }

            try {
                jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
                jdbcTemplate.execute("ALTER TABLE users ADD CONSTRAINT users_role_check " +
                        "CHECK (role IN ('MASTER_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PRINCIPAL', 'TEACHER', 'STUDENT', 'PARENT', 'STAFF', 'ACCOUNTANT', 'LIBRARIAN'))");
            } catch (Exception e) {
                log.warn("Notice updating users_role_check: {}", e.getMessage());
            }

            // Ensure teacher table columns and join tables for multiselect classes & subjects
            try {
                jdbcTemplate.execute("ALTER TABLE teachers ALTER COLUMN subject TYPE VARCHAR(500)");
            } catch (Exception ignored) {}
            try {
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS assigned_class VARCHAR(500)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS assigned_class_ids VARCHAR(500)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS subject_ids VARCHAR(500)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS gender VARCHAR(16)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS dob DATE");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS father_name VARCHAR(128)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS mother_name VARCHAR(128)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS marital_status VARCHAR(32)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS contract_type VARCHAR(32)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS shift VARCHAR(32)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS work_location VARCHAR(128)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS height VARCHAR(32)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS weight VARCHAR(32)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS bank_account_number VARCHAR(64)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS bank_name VARCHAR(128)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS ifsc_code VARCHAR(32)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS national_id_number VARCHAR(64)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS doc_name VARCHAR(128)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS prev_school_name VARCHAR(128)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS prev_school_address VARCHAR(255)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS permanent_address VARCHAR(255)");
                jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS teacher_bio TEXT");
            } catch (Exception e) {
                log.warn("Notice updating teachers columns: {}", e.getMessage());
            }

            try {
                jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS teacher_classes (" +
                        "teacher_id BIGINT NOT NULL, " +
                        "class_id BIGINT NOT NULL, " +
                        "PRIMARY KEY (teacher_id, class_id))");
            } catch (Exception ignored) {}

            try {
                jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS teacher_subjects (" +
                        "teacher_id BIGINT NOT NULL, " +
                        "subject_id BIGINT NOT NULL, " +
                        "PRIMARY KEY (teacher_id, subject_id))");
            } catch (Exception ignored) {}

            // Purge legacy static records that were seeded by backend code
            try {
                jdbcTemplate.execute("DELETE FROM homeworks WHERE title IN ('Quadratic Equations', 'Solar System Project', 'Persuasive Essay Writing', 'French Revolution Timeline')");
            } catch (Exception ignored) {}

            try {
                jdbcTemplate.execute("DELETE FROM fee_collections WHERE admission_no = 'ADM-24' AND (note LIKE 'TXN-%' OR note = 'Academic tuition and lab fees')");
            } catch (Exception ignored) {}

            try {
                jdbcTemplate.execute("DELETE FROM exam_schedules WHERE exam_name IN ('Term 1 Midterm Examination', 'Term 1 Science Assessment', 'Term 1 English Literature')");
            } catch (Exception ignored) {}

            try {
                jdbcTemplate.execute("DELETE FROM exam_results WHERE admission_no = 'ADM-24' AND exam IN ('Weekly Math Quiz', 'Weekly Science Test')");
            } catch (Exception ignored) {}

            try {
                jdbcTemplate.execute("DELETE FROM leave_requests WHERE reason IN ('Medical appointment - scheduled follow-up', 'Family function - cousin''s wedding', 'Personal leave - urgent personal work')");
            } catch (Exception ignored) {}

            try {
                jdbcTemplate.execute("DELETE FROM notices WHERE title IN ('Annual Sports Day 2026 Registration Open', 'Mid-Term Examination Schedule Notification', 'Parent Teacher Meeting (PTM) Details')");
            } catch (Exception ignored) {}

            try {
                jdbcTemplate.execute("DELETE FROM staff WHERE email IN ('principal@school.com', 'accountant@school.com', 'librarian@school.com')");
            } catch (Exception ignored) {}
        };
    }
}
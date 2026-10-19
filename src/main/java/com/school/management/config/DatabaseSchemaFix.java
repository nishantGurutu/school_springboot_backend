package com.school.management.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.CommandLineRunner;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Fixes pre-existing database constraints without deleting school records.
 */
@Configuration
@Slf4j
public class DatabaseSchemaFix {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaFix(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

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
                        "CHECK (role IN ('MASTER_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PRINCIPAL', 'TEACHER', 'STUDENT', 'PARENT', 'STAFF', 'ACCOUNTANT', 'LIBRARIAN', 'DRIVER'))");
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

            // Schema additions for teacher assignments and student submissions
            try {
                jdbcTemplate.execute("ALTER TABLE homeworks ADD COLUMN IF NOT EXISTS teacher_id BIGINT");
                jdbcTemplate.execute("ALTER TABLE homeworks ADD COLUMN IF NOT EXISTS subject_id BIGINT");
                jdbcTemplate.execute("ALTER TABLE homeworks ADD COLUMN IF NOT EXISTS target_student_id BIGINT");
                jdbcTemplate.execute("ALTER TABLE homeworks ADD COLUMN IF NOT EXISTS target_student_name VARCHAR(255)");
                jdbcTemplate.execute("ALTER TABLE homeworks ADD COLUMN IF NOT EXISTS target_student_admission_no VARCHAR(64)");
                jdbcTemplate.execute("ALTER TABLE homeworks ADD COLUMN IF NOT EXISTS points INT DEFAULT 100");
                jdbcTemplate.execute("ALTER TABLE homeworks ADD COLUMN IF NOT EXISTS file_attachment_url VARCHAR(500)");
            } catch (Exception ignored) {}

            try {
                jdbcTemplate.execute("ALTER TABLE homework_submissions ADD COLUMN IF NOT EXISTS student_name VARCHAR(255)");
                jdbcTemplate.execute("ALTER TABLE homework_submissions ADD COLUMN IF NOT EXISTS student_roll_no VARCHAR(64)");
                jdbcTemplate.execute("ALTER TABLE homework_submissions ADD COLUMN IF NOT EXISTS student_admission_no VARCHAR(64)");
                jdbcTemplate.execute("ALTER TABLE homework_submissions ADD COLUMN IF NOT EXISTS submission_text TEXT");
                jdbcTemplate.execute("ALTER TABLE homework_submissions ADD COLUMN IF NOT EXISTS file_url VARCHAR(500)");
                jdbcTemplate.execute("ALTER TABLE homework_submissions ADD COLUMN IF NOT EXISTS status VARCHAR(32) DEFAULT 'SUBMITTED'");
                jdbcTemplate.execute("ALTER TABLE homework_submissions ADD COLUMN IF NOT EXISTS points INT");
                jdbcTemplate.execute("ALTER TABLE homework_submissions ADD COLUMN IF NOT EXISTS teacher_feedback TEXT");
                jdbcTemplate.execute("ALTER TABLE homework_submissions ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP");
            } catch (Exception ignored) {}

            // Ensure transport_trips table has the start location and daily checklist columns
            try {
                jdbcTemplate.execute("ALTER TABLE transport_trips ADD COLUMN IF NOT EXISTS start_latitude DOUBLE PRECISION");
                jdbcTemplate.execute("ALTER TABLE transport_trips ADD COLUMN IF NOT EXISTS start_longitude DOUBLE PRECISION");
                jdbcTemplate.execute("ALTER TABLE transport_trips ADD COLUMN IF NOT EXISTS start_location_name VARCHAR(256)");
                jdbcTemplate.execute("ALTER TABLE transport_trips ADD COLUMN IF NOT EXISTS chk_brakes BOOLEAN DEFAULT TRUE");
                jdbcTemplate.execute("ALTER TABLE transport_trips ADD COLUMN IF NOT EXISTS chk_lights BOOLEAN DEFAULT TRUE");
                jdbcTemplate.execute("ALTER TABLE transport_trips ADD COLUMN IF NOT EXISTS chk_tires BOOLEAN DEFAULT TRUE");
                jdbcTemplate.execute("ALTER TABLE transport_trips ADD COLUMN IF NOT EXISTS chk_emergency_kit BOOLEAN DEFAULT TRUE");
                jdbcTemplate.execute("ALTER TABLE transport_trips ADD COLUMN IF NOT EXISTS chk_gps_active BOOLEAN DEFAULT TRUE");
            } catch (Exception e) {
                log.warn("Notice updating transport_trips columns: {}", e.getMessage());
            }
        };
    }
}
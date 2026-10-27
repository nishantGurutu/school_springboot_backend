package com.school.management.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

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

            // Ensure teacher_subjects foreign keys cascade delete when a subject or teacher is deleted
            try {
                jdbcTemplate.execute("DELETE FROM teacher_subjects WHERE subject_id NOT IN (SELECT id FROM subjects)");
                jdbcTemplate.execute("DELETE FROM teacher_subjects WHERE teacher_id NOT IN (SELECT id FROM teachers)");

                jdbcTemplate.execute("DO $$\n" +
                        "DECLARE r RECORD;\n" +
                        "BEGIN\n" +
                        "  FOR r IN (\n" +
                        "    SELECT tc.constraint_name\n" +
                        "    FROM information_schema.table_constraints tc\n" +
                        "    JOIN information_schema.key_column_usage kcu\n" +
                        "      ON tc.constraint_name = kcu.constraint_name\n" +
                        "      AND tc.table_schema = kcu.table_schema\n" +
                        "    WHERE tc.constraint_type = 'FOREIGN KEY'\n" +
                        "      AND tc.table_name = 'teacher_subjects'\n" +
                        "      AND kcu.column_name = 'subject_id'\n" +
                        "  ) LOOP\n" +
                        "    EXECUTE 'ALTER TABLE teacher_subjects DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name);\n" +
                        "  END LOOP;\n" +
                        "END $$;");

                jdbcTemplate.execute("ALTER TABLE teacher_subjects ADD CONSTRAINT fk_teacher_subjects_subject " +
                        "FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE");
            } catch (Exception e) {
                log.warn("Notice updating teacher_subjects subject_id foreign key: {}", e.getMessage());
            }

            try {
                jdbcTemplate.execute("DO $$\n" +
                        "DECLARE r RECORD;\n" +
                        "BEGIN\n" +
                        "  FOR r IN (\n" +
                        "    SELECT tc.constraint_name\n" +
                        "    FROM information_schema.table_constraints tc\n" +
                        "    JOIN information_schema.key_column_usage kcu\n" +
                        "      ON tc.constraint_name = kcu.constraint_name\n" +
                        "      AND tc.table_schema = kcu.table_schema\n" +
                        "    WHERE tc.constraint_type = 'FOREIGN KEY'\n" +
                        "      AND tc.table_name = 'teacher_subjects'\n" +
                        "      AND kcu.column_name = 'teacher_id'\n" +
                        "  ) LOOP\n" +
                        "    EXECUTE 'ALTER TABLE teacher_subjects DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name);\n" +
                        "  END LOOP;\n" +
                        "END $$;");

                jdbcTemplate.execute("ALTER TABLE teacher_subjects ADD CONSTRAINT fk_teacher_subjects_teacher " +
                        "FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE");
            } catch (Exception e) {
                log.warn("Notice updating teacher_subjects teacher_id foreign key: {}", e.getMessage());
            }

            // Ensure teacher_classes foreign keys cascade delete when a class or teacher is deleted
            try {
                jdbcTemplate.execute("DELETE FROM teacher_classes WHERE class_id NOT IN (SELECT id FROM school_classes)");
                jdbcTemplate.execute("DELETE FROM teacher_classes WHERE teacher_id NOT IN (SELECT id FROM teachers)");

                jdbcTemplate.execute("DO $$\n" +
                        "DECLARE r RECORD;\n" +
                        "BEGIN\n" +
                        "  FOR r IN (\n" +
                        "    SELECT tc.constraint_name\n" +
                        "    FROM information_schema.table_constraints tc\n" +
                        "    JOIN information_schema.key_column_usage kcu\n" +
                        "      ON tc.constraint_name = kcu.constraint_name\n" +
                        "      AND tc.table_schema = kcu.table_schema\n" +
                        "    WHERE tc.constraint_type = 'FOREIGN KEY'\n" +
                        "      AND tc.table_name = 'teacher_classes'\n" +
                        "      AND kcu.column_name = 'class_id'\n" +
                        "  ) LOOP\n" +
                        "    EXECUTE 'ALTER TABLE teacher_classes DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name);\n" +
                        "  END LOOP;\n" +
                        "END $$;");

                jdbcTemplate.execute("ALTER TABLE teacher_classes ADD CONSTRAINT fk_teacher_classes_class " +
                        "FOREIGN KEY (class_id) REFERENCES school_classes(id) ON DELETE CASCADE");
            } catch (Exception e) {
                log.warn("Notice updating teacher_classes class_id foreign key: {}", e.getMessage());
            }

            try {
                jdbcTemplate.execute("DO $$\n" +
                        "DECLARE r RECORD;\n" +
                        "BEGIN\n" +
                        "  FOR r IN (\n" +
                        "    SELECT tc.constraint_name\n" +
                        "    FROM information_schema.table_constraints tc\n" +
                        "    JOIN information_schema.key_column_usage kcu\n" +
                        "      ON tc.constraint_name = kcu.constraint_name\n" +
                        "      AND tc.table_schema = kcu.table_schema\n" +
                        "    WHERE tc.constraint_type = 'FOREIGN KEY'\n" +
                        "      AND tc.table_name = 'teacher_classes'\n" +
                        "      AND kcu.column_name = 'teacher_id'\n" +
                        "  ) LOOP\n" +
                        "    EXECUTE 'ALTER TABLE teacher_classes DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name);\n" +
                        "  END LOOP;\n" +
                        "END $$;");

                jdbcTemplate.execute("ALTER TABLE teacher_classes ADD CONSTRAINT fk_teacher_classes_teacher " +
                        "FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE");
            } catch (Exception e) {
                log.warn("Notice updating teacher_classes teacher_id foreign key: {}", e.getMessage());
            }

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

            // Transport Driver Profile normalization and cleanup of fake/demo records
            try {
                jdbcTemplate.execute("UPDATE driver_profiles SET school_id = 'SCH-001' WHERE school_id = 'SCH001' OR school_id IS NULL");
                jdbcTemplate.execute("UPDATE buses SET school_id = 'SCH-001' WHERE school_id = 'SCH001' OR school_id IS NULL");
                jdbcTemplate.execute("UPDATE transport_routes SET school_id = 'SCH-001' WHERE school_id = 'SCH001' OR school_id IS NULL");
                jdbcTemplate.execute("UPDATE bus_assignments SET school_id = 'SCH-001' WHERE school_id = 'SCH001' OR school_id IS NULL");

                // Clean up fake/mock demo drivers (Ramesh Kumar and Suresh Yadav) that are not in staff or teachers
                jdbcTemplate.execute("DELETE FROM bus_location_history WHERE trip_id IN (SELECT id FROM transport_trips WHERE driver_id IN (SELECT id FROM driver_profiles WHERE email IN ('driver@school.com', 'suresh.driver@school.com')))");
                jdbcTemplate.execute("DELETE FROM trip_stop_events WHERE trip_id IN (SELECT id FROM transport_trips WHERE driver_id IN (SELECT id FROM driver_profiles WHERE email IN ('driver@school.com', 'suresh.driver@school.com')))");
                jdbcTemplate.execute("DELETE FROM transport_alerts WHERE trip_id IN (SELECT id FROM transport_trips WHERE driver_id IN (SELECT id FROM driver_profiles WHERE email IN ('driver@school.com', 'suresh.driver@school.com')))");
                jdbcTemplate.execute("DELETE FROM transport_trips WHERE driver_id IN (SELECT id FROM driver_profiles WHERE email IN ('driver@school.com', 'suresh.driver@school.com'))");
                jdbcTemplate.execute("DELETE FROM bus_assignments WHERE driver_id IN (SELECT id FROM driver_profiles WHERE email IN ('driver@school.com', 'suresh.driver@school.com'))");
                jdbcTemplate.execute("DELETE FROM driver_profiles WHERE email IN ('driver@school.com', 'suresh.driver@school.com')");
            } catch (Exception e) {
                log.warn("Notice updating transport driver profiles: {}", e.getMessage());
            }
        };
    }
}
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
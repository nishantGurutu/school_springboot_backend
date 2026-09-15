package com.school.management.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.CommandLineRunner;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Fixes pre-existing database constraints that the schema generator cannot evolve.
 * The old Postgres `users` table has a `users_role_check` CHECK constraint that lacks
 * the newly added PARENT role, so its enum must be rebuilt at startup (idempotently).
 */
@Configuration
@RequiredArgsConstructor
public class DatabaseSchemaFix {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    public CommandLineRunner fixRoleCheckConstraint() {
        return args -> {
            String product;
            try (Connection conn = dataSource.getConnection()) {
                product = conn.getMetaData().getDatabaseProductName();
            }
            if (product == null || !product.toLowerCase().contains("postgres")) {
                return;
            }
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
            jdbcTemplate.execute("ALTER TABLE users ADD CONSTRAINT users_role_check " +
                    "CHECK (role IN ('MASTER_ADMIN', 'ADMIN', 'TEACHER', 'STUDENT', 'PARENT'))");
        };
    }
}
package com.school.management.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Hibernate schema update does not remove the legacy NOT NULL constraint. */
@Component
@RequiredArgsConstructor
public class ChapterNotesSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        // Chapter notes have chapter_id and no topic_id. Keep the topic foreign key
        // so notes attached to a topic continue to reference a valid topic.
        jdbcTemplate.execute("ALTER TABLE topic_notes ALTER COLUMN topic_id DROP NOT NULL");
    }
}

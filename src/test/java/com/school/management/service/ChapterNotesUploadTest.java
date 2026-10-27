package com.school.management.service;

import com.school.management.config.ChapterNotesSchemaMigration;
import com.school.management.entity.*;
import com.school.management.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import com.school.management.util.SecurityUtil;
import jakarta.persistence.EntityManager;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc(addFilters = false)
class ChapterNotesUploadTest {
    @Autowired CurriculumService curriculum;
    @Autowired SubjectRepository subjects;
    @Autowired SubjectChapterRepository chapters;
    @Autowired TopicNoteRepository notes;
    @MockBean FileStorageService storage;
    @MockBean SecurityUtil security;
    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;

    @Test void multipartAndJsonResponsesSerializeLazyRelationsAsIds() throws Exception {
        var subject = subjects.save(SubjectEntity.builder().name("HTTP Math").code("NOTES-HTTP-MATH").className("Class 7").build());
        var chapter = chapters.save(SubjectChapterEntity.builder().subject(subject).className("Class 7")
                .chapterNumber("Chapter 1").chapterTitle("Fractions").build());
        entityManager.flush();
        entityManager.clear();
        var file = new MockMultipartFile("file", "fractions.pdf", "application/pdf", "%PDF-1.4 test".getBytes());
        when(storage.storeFile(any(), eq("notes"))).thenReturn(new FileStorageService.UploadResult(
                "fractions.pdf", "stored.pdf", "/api/files/download/stored.pdf", "PDF", 14L, "14 B"));
        mvc.perform(multipart("/api/teacher/curriculum/notes").file(file)
                .param("chapterId", chapter.getId().toString()).param("title", "Worked examples"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.chapterId").value(chapter.getId()))
                .andExpect(jsonPath("$.subjectId").value(subject.getId()))
                .andExpect(jsonPath("$.fileUrl").value("/api/files/download/stored.pdf"))
                .andExpect(jsonPath("$.chapter").doesNotExist())
                .andExpect(jsonPath("$.hibernateLazyInitializer").doesNotExist());
        mvc.perform(post("/api/teacher/curriculum/notes/json").contentType("application/json")
                .content("{\"chapterId\":" + chapter.getId() + ",\"title\":\"Link note\",\"directUrl\":\"https://example.test/notes.pdf\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileUrl").value("https://example.test/notes.pdf"))
                .andExpect(jsonPath("$.chapterId").value(chapter.getId()));
    }

    @Test void legacyTopicConstraintIsRemovedIdempotentlyWithoutDeletingNotes() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:notes_migration;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE topic_notes (id BIGINT PRIMARY KEY, topic_id BIGINT NOT NULL)");
        jdbc.update("INSERT INTO topic_notes VALUES (1, 7)");
        var migration = new ChapterNotesSchemaMigration(jdbc);
        migration.run(null);
        migration.run(null);
        jdbc.update("INSERT INTO topic_notes VALUES (2, NULL)");
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM topic_notes", Integer.class));
        assertEquals(7L, jdbc.queryForObject("SELECT topic_id FROM topic_notes WHERE id=1", Long.class));
    }

    @Test void uploadedPdfPersistsDirectlyOnChapterWithoutTopic() {
        var subject = subjects.save(SubjectEntity.builder().name("Notes Math").code("NOTES-UPLOAD-MATH").className("Class 7").build());
        var chapter = chapters.save(SubjectChapterEntity.builder().subject(subject).className("Class 7")
                .chapterNumber("Chapter 1").chapterTitle("Fractions").build());
        var file = new MockMultipartFile("file", "fractions.pdf", "application/pdf", "%PDF-1.4 test".getBytes());
        when(storage.storeFile(file, "notes")).thenReturn(new FileStorageService.UploadResult(
                "fractions.pdf", "stored.pdf", "/api/files/download/stored.pdf", "PDF", file.getSize(), "14 B"));
        var saved = curriculum.addNote(chapter.getId(), null, "Worked examples", "Teacher notes", null,
                file, null, true, null, "Teacher");
        notes.flush();
        var persisted = notes.findById(saved.getId()).orElseThrow();
        assertNull(persisted.getTopic());
        assertEquals(chapter.getId(), persisted.getChapter().getId());
        assertEquals(subject.getId(), persisted.getSubject().getId());
        assertEquals("Class 7", persisted.getClassName());
        assertEquals("/api/files/download/stored.pdf", persisted.getFileUrl());
        assertEquals(1, notes.findByChapterIdOrderByCreatedAtDesc(chapter.getId()).size());
    }
}

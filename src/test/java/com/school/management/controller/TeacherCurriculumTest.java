package com.school.management.controller;

import com.school.management.entity.SubjectChapterEntity;
import com.school.management.entity.SubjectEntity;
import com.school.management.service.CurriculumService;
import com.school.management.util.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherCurriculumTest {

    @Mock
    private CurriculumService curriculumService;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private TeacherCurriculumController controller;

    @Test
    void createChapterWithValidDataReturnsDto() {
        SubjectEntity subject = SubjectEntity.builder().id(2L).name("Science").code("SCI-10").build();
        SubjectChapterEntity chapter = SubjectChapterEntity.builder()
                .id(10L)
                .subject(subject)
                .className("Class 10")
                .chapterNumber("Chapter 1")
                .chapterTitle("Chemical Reactions")
                .description("Basics")
                .displayOrder(1)
                .build();

        Map<String, Object> dto = new HashMap<>();
        dto.put("id", 10L);
        dto.put("subjectId", 2L);
        dto.put("subjectName", "Science");
        dto.put("chapterNumber", "Chapter 1");
        dto.put("chapterTitle", "Chemical Reactions");
        dto.put("className", "Class 10");
        dto.put("notes", Collections.emptyList());
        dto.put("topics", Collections.emptyList());

        when(curriculumService.createChapter(eq(2L), eq("Class 10"), eq("Chapter 1"), eq("Chemical Reactions"), eq("Basics"), eq(1)))
                .thenReturn(chapter);
        when(curriculumService.mapChapterToDto(chapter)).thenReturn(dto);

        Map<String, Object> payload = new HashMap<>();
        payload.put("subjectId", 2);
        payload.put("className", "Class 10");
        payload.put("chapterNumber", "Chapter 1");
        payload.put("chapterTitle", "Chemical Reactions");
        payload.put("description", "Basics");
        payload.put("displayOrder", 1);

        ResponseEntity<Map<String, Object>> res = controller.createChapter(payload);

        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
        assertEquals("Chapter 1", res.getBody().get("chapterNumber"));
        assertEquals("Chemical Reactions", res.getBody().get("chapterTitle"));
        assertEquals(2L, res.getBody().get("subjectId"));
    }

    @Test
    void createChapterWithSnakeCaseAndAlternativeKeysWorks() {
        SubjectEntity subject = SubjectEntity.builder().id(3L).name("Math").code("MTH-10").build();
        SubjectChapterEntity chapter = SubjectChapterEntity.builder()
                .id(11L)
                .subject(subject)
                .className("Class 9")
                .chapterNumber("Chapter 2")
                .chapterTitle("Polynomials")
                .displayOrder(2)
                .build();

        Map<String, Object> dto = new HashMap<>();
        dto.put("id", 11L);
        dto.put("subjectId", 3L);
        dto.put("chapterTitle", "Polynomials");

        when(curriculumService.createChapter(eq(3L), eq("Class 9"), eq("Chapter 2"), eq("Polynomials"), isNull(), eq(2)))
                .thenReturn(chapter);
        when(curriculumService.mapChapterToDto(chapter)).thenReturn(dto);

        Map<String, Object> payload = new HashMap<>();
        payload.put("subject_id", "3");
        payload.put("class_name", "Class 9");
        payload.put("chapter_number", "Chapter 2");
        payload.put("title", "Polynomials");
        payload.put("display_order", "2");

        ResponseEntity<Map<String, Object>> res = controller.createChapter(payload);

        assertEquals(200, res.getStatusCode().value());
        assertEquals("Polynomials", res.getBody().get("chapterTitle"));
    }

    @Test
    void createChapterMissingSubjectOrTitleReturnsBadRequest() {
        Map<String, Object> noSubject = Map.of("chapterTitle", "Trigonometry");
        ResponseEntity<Map<String, Object>> res1 = controller.createChapter(noSubject);
        assertEquals(400, res1.getStatusCode().value());

        Map<String, Object> noTitle = Map.of("subjectId", 1L);
        ResponseEntity<Map<String, Object>> res2 = controller.createChapter(noTitle);
        assertEquals(400, res2.getStatusCode().value());
    }
}

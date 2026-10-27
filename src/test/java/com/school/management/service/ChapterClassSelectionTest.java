package com.school.management.service;

import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChapterClassSelectionTest {
    @Mock SubjectRepository subjectRepository;
    @Mock SubjectChapterRepository chapterRepository;
    @InjectMocks CurriculumService service;

    @Test void classWithoutSubjectsReturnsEmptyRatherThanGlobalFallback() {
        when(subjectRepository.findByClassNameIgnoreCase("Class 7")).thenReturn(List.of());
        assertTrue(service.getTeacherSubjects(null, null, "Class 7").isEmpty());
        verify(subjectRepository, never()).findAll();
    }

    @Test void chapterSavesSelectedClassAndSubject() {
        var subject = SubjectEntity.builder().id(1L).name("Math").className("Class 7").build();
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(chapterRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var chapter = service.createChapter(1L, "Class 7", "Chapter 1", "Fractions", "Overview", 1);
        assertSame(subject, chapter.getSubject());
        assertEquals("Class 7", chapter.getClassName());
        assertEquals("Fractions", chapter.getChapterTitle());
    }

    @Test void rejectsSubjectFromAnotherClassBeforeCreatingChapter() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(
                SubjectEntity.builder().id(1L).className("Class 8").build()));
        assertThrows(BadRequestException.class,
                () -> service.createChapter(1L, "Class 7", "Chapter 1", "Fractions", null, 1));
        verifyNoInteractions(chapterRepository);
    }

    @Test void editCannotMoveChapterIntoAnotherSubjectsClass() {
        var subject = SubjectEntity.builder().id(1L).className("Class 7").build();
        var chapter = SubjectChapterEntity.builder().id(2L).subject(subject).className("Class 7").chapterTitle("Fractions").build();
        when(chapterRepository.findById(2L)).thenReturn(Optional.of(chapter));
        assertThrows(BadRequestException.class,
                () -> service.updateChapter(2L, "Chapter 2", "Changed", null, 2, "Class 8", null));
        assertEquals("Fractions", chapter.getChapterTitle());
        verify(chapterRepository, never()).save(any());
    }
}

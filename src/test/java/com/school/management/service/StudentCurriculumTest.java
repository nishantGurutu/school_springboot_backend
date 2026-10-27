package com.school.management.service;

import com.school.management.domain.user.Role;
import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentCurriculumTest {
    @Mock SubjectRepository subjectRepository;
    @Mock SubjectChapterRepository chapterRepository;
    @Mock SubjectTopicRepository topicRepository;
    @Mock TopicNoteRepository noteRepository;
    @Mock StudentRepository studentRepository;
    @Mock SecurityUtil securityUtil;
    @InjectMocks CurriculumService service;

    private void student() {
        when(securityUtil.getCurrentUser()).thenReturn(Optional.of(UserEntity.builder().role(Role.STUDENT).email("s@school.test").build()));
        when(studentRepository.findByEmailIgnoreCase("s@school.test")).thenReturn(Optional.of(StudentEntity.builder().className("10").subjectIds("1").build()));
    }

    @Test void noMatchingChapterReturnsEmptyInsteadOfAnotherClass() {
        student();
        var subject = SubjectEntity.builder().id(1L).name("Math").className("10").build();
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(chapterRepository.findBySubjectIdOrderByDisplayOrderAsc(1L)).thenReturn(List.of(SubjectChapterEntity.builder().id(8L).subject(subject).className("1").build()));
        var result = service.getSubjectCurriculumForStudent(1L, "1");
        assertEquals(0, result.get("totalChapters"));
        verifyNoInteractions(noteRepository);
    }

    @Test void topicFromAnotherClassIsForbidden() {
        student();
        var subject = SubjectEntity.builder().id(1L).name("Math").className("10").build();
        var chapter = SubjectChapterEntity.builder().id(8L).subject(subject).className("9").build();
        when(topicRepository.findById(4L)).thenReturn(Optional.of(SubjectTopicEntity.builder().id(4L).subject(subject).chapter(chapter).build()));
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.getTopicNotesForStudent(4L)).getStatusCode().value());
        verifyNoInteractions(noteRepository);
    }

    @Test void ownTopicReturnsTeacherHtmlAndAttachment() {
        student();
        var subject = SubjectEntity.builder().id(1L).name("Math").className("10").build();
        var chapter = SubjectChapterEntity.builder().id(8L).subject(subject).className("10").chapterTitle("Fractions").build();
        var topic = SubjectTopicEntity.builder().id(4L).title("Addition").subject(subject).chapter(chapter).build();
        when(topicRepository.findById(4L)).thenReturn(Optional.of(topic));
        when(noteRepository.findByTopicIdOrderByCreatedAtDesc(4L)).thenReturn(List.of(TopicNoteEntity.builder().id(2L).title("Worked examples").contentHtml("<p>Teacher content</p>").fileUrl("/api/files/download/math.pdf").build()));
        var response = service.getTopicNotesForStudent(4L);
        assertEquals("Addition", response.get("topicTitle"));
        var notes = (List<?>) response.get("notes");
        assertEquals("<p>Teacher content</p>", ((Map<?, ?>) notes.get(0)).get("contentHtml"));
    }
}

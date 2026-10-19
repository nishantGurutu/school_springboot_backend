package com.school.management.controller;

import com.school.management.domain.user.Role;
import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeworkSubmissionTest {
    @Mock SecurityUtil securityUtil;
    @Mock StudentRepository studentRepository;
    @Mock HomeworkRepository homeworkRepository;
    @Mock HomeworkSubmissionRepository homeworkSubmissionRepository;
    @org.mockito.Mock com.school.management.service.PortalNotificationService portalNotifications;
    @org.mockito.Mock com.school.management.repository.UserRepository portalUsers;
    @InjectMocks MobileAppController controller;

    @Test void parentCannotSubmitForStudent() {
        when(securityUtil.getCurrentUser()).thenReturn(Optional.of(UserEntity.builder().role(Role.PARENT).build()));
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> controller.submitHomework("hw_1")).getStatusCode().value());
        verifyNoInteractions(homeworkSubmissionRepository);
    }

    @Test void rejectsHomeworkFromAnotherClass() {
        when(securityUtil.getCurrentUser()).thenReturn(Optional.of(UserEntity.builder().role(Role.STUDENT).email("student@school.test").build()));
        when(studentRepository.findByEmailIgnoreCase("student@school.test")).thenReturn(Optional.of(StudentEntity.builder().id(7L).className("10").section("A").build()));
        when(homeworkRepository.findById(1L)).thenReturn(Optional.of(HomeworkEntity.builder().id(1L).className("9").build()));
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> controller.submitHomework("hw_1")).getStatusCode().value());
        verifyNoInteractions(homeworkSubmissionRepository);
    }

    @Test void persistsSubmissionForLoggedInStudent() {
        when(securityUtil.getCurrentUser()).thenReturn(Optional.of(UserEntity.builder().role(Role.STUDENT).email("student@school.test").build()));
        when(studentRepository.findByEmailIgnoreCase("student@school.test")).thenReturn(Optional.of(StudentEntity.builder().id(7L).className("10").section("A").build()));
        when(homeworkRepository.findById(1L)).thenReturn(Optional.of(HomeworkEntity.builder().id(1L).className("10").section("A").build()));
        when(homeworkSubmissionRepository.findByHomeworkIdAndStudentId(1L, 7L)).thenReturn(Optional.empty());
        controller.submitHomework("hw_1");
        ArgumentCaptor<HomeworkSubmissionEntity> saved = ArgumentCaptor.forClass(HomeworkSubmissionEntity.class);
        verify(homeworkSubmissionRepository).save(saved.capture());
        assertEquals(7L, saved.getValue().getStudentId());
        assertEquals(1L, saved.getValue().getHomeworkId());
        assertNotNull(saved.getValue().getSubmittedAt());
    }
    @Test void studentCannotOverwriteTeacherReviewedWork() {
        when(securityUtil.getCurrentUser()).thenReturn(Optional.of(UserEntity.builder().role(Role.STUDENT).email("student@school.test").build()));
        when(studentRepository.findByEmailIgnoreCase("student@school.test")).thenReturn(Optional.of(StudentEntity.builder().id(7L).className("10").section("A").build()));
        when(homeworkRepository.findById(1L)).thenReturn(Optional.of(HomeworkEntity.builder().id(1L).className("10").section("A").build()));
        when(homeworkSubmissionRepository.findByHomeworkIdAndStudentId(1L, 7L)).thenReturn(Optional.of(HomeworkSubmissionEntity.builder().homeworkId(1L).studentId(7L).status("GRADED").points(95).build()));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> controller.submitStudentAssignment("hw_1", java.util.Map.of("submissionText", "replacement"))).getStatusCode().value());
        verify(homeworkSubmissionRepository, never()).save(any());
    }
}

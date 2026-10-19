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
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentAccessTest {
    @Mock SecurityUtil securityUtil;
    @Mock StudentRepository studentRepository;
    @Mock GuardianRepository guardianRepository;
    @Mock TeacherRepository teacherRepository;
    @Mock SchoolClassRepository schoolClassRepository;
    @Mock HomeworkRepository homeworkRepository;
    @Mock HomeworkSubmissionRepository homeworkSubmissionRepository;
    @org.mockito.Mock com.school.management.service.PortalNotificationService portalNotifications;
    @org.mockito.Mock com.school.management.repository.UserRepository portalUsers;
    @InjectMocks MobileAppController controller;

    private void login(Role role) {
        when(securityUtil.getCurrentUser()).thenReturn(Optional.of(UserEntity.builder().role(role).email("user@test.local").build()));
    }
    @Test void studentCannotReadAnotherAdmissionNumber() {
        login(Role.STUDENT);
        when(studentRepository.findByEmailIgnoreCase("user@test.local")).thenReturn(Optional.of(StudentEntity.builder().id(1L).admissionNo("OWN").build()));
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> controller.getStudentAssignments("OTHER")).getStatusCode().value());
        verifyNoInteractions(homeworkRepository);
    }
    @Test void parentCannotReadUnlinkedChild() {
        login(Role.PARENT);
        when(studentRepository.findByGuardianEmailIgnoreCase("user@test.local")).thenReturn(List.of());
        when(guardianRepository.findByEmailIgnoreCase("user@test.local")).thenReturn(Optional.empty());
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> controller.getStudentAssignments("OTHER")).getStatusCode().value());
        verifyNoInteractions(homeworkRepository);
    }
    @Test void parentSeesClassAndPersonalWorkForLinkedChildOnly() {
        login(Role.PARENT);
        StudentEntity child = StudentEntity.builder().id(1L).admissionNo("OWN").className("7").section("B").build();
        when(studentRepository.findByGuardianEmailIgnoreCase("user@test.local")).thenReturn(List.of(child));
        when(guardianRepository.findByEmailIgnoreCase("user@test.local")).thenReturn(Optional.empty());
        when(homeworkRepository.findAllByOrderByIdDesc()).thenReturn(List.of(
            HomeworkEntity.builder().id(1L).className("7").section("B").build(),
            HomeworkEntity.builder().id(2L).className("7").section("A").build(),
            HomeworkEntity.builder().id(3L).className("7").section("B").targetStudentId(2L).build(),
            HomeworkEntity.builder().id(4L).className("7").section("B").targetStudentId(1L).build()));
        assertEquals(List.of("hw_1", "hw_4"), controller.getStudentAssignments("OWN").getBody().stream().map(row -> row.get("id")).toList());
    }
    @Test void studentCannotSubmitAnotherStudentsTargetedAssignment() {
        login(Role.STUDENT);
        when(studentRepository.findByEmailIgnoreCase("user@test.local")).thenReturn(Optional.of(StudentEntity.builder().id(1L).className("7").section("B").build()));
        when(homeworkRepository.findById(3L)).thenReturn(Optional.of(HomeworkEntity.builder().id(3L).className("7").section("B").targetStudentId(2L).build()));
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> controller.submitStudentAssignment("hw_3", Map.of("submissionText", "answer"))).getStatusCode().value());
        verifyNoInteractions(homeworkSubmissionRepository);
    }
    @Test void studentCannotCreateAnAssignment() {
        login(Role.STUDENT);
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> controller.createTeacherAssignment(Map.of())).getStatusCode().value());
        verifyNoInteractions(homeworkRepository);
    }
    @Test void teacherCannotPublishToUnassignedClass() {
        login(Role.TEACHER);
        when(teacherRepository.findByEmailIgnoreCase("user@test.local")).thenReturn(Optional.of(TeacherEntity.builder().id(2L).build()));
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> controller.createTeacherAssignment(Map.of("title", "Homework", "subject", "Math", "className", "7", "dueDate", LocalDate.now().plusDays(1).toString()))).getStatusCode().value());
        verifyNoInteractions(homeworkRepository);
    }
    @Test void reviewCannotUseSubmissionFromDifferentAssignment() {
        login(Role.ADMIN);
        when(homeworkRepository.findById(1L)).thenReturn(Optional.of(HomeworkEntity.builder().id(1L).className("7").build()));
        when(homeworkSubmissionRepository.findById(5L)).thenReturn(Optional.of(HomeworkSubmissionEntity.builder().id(5L).homeworkId(2L).build()));
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> controller.reviewSubmission("hw_1", 5L, Map.of("status", "GRADED", "points", 50))).getStatusCode().value());
        verify(homeworkSubmissionRepository, never()).save(any());
    }
    @Test void rejectsScoreAboveMaximum() {
        login(Role.ADMIN);
        when(homeworkRepository.findById(1L)).thenReturn(Optional.of(HomeworkEntity.builder().id(1L).className("7").points(20).build()));
        when(homeworkSubmissionRepository.findById(5L)).thenReturn(Optional.of(HomeworkSubmissionEntity.builder().id(5L).homeworkId(1L).build()));
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> controller.reviewSubmission("hw_1", 5L, Map.of("status", "GRADED", "points", 21))).getStatusCode().value());
        verify(homeworkSubmissionRepository, never()).save(any());
    }
}

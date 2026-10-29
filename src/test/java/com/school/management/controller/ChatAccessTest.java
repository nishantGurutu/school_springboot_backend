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
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatAccessTest {
    @Mock SecurityUtil security;
    @Mock UserRepository users;
    @Mock StudentRepository students;
    @Mock GuardianRepository guardians;
    @Mock TeacherRepository teachers;
    @Mock ChatMessageRepository messages;
    @org.mockito.Mock com.school.management.service.PortalNotificationService portalNotifications;
    ChatController controller;
    @org.junit.jupiter.api.BeforeEach void setUp() {
        controller = new ChatController(security,
            new com.school.management.chat.ChatPolicy(security, users, students, guardians, teachers),
            users, students, guardians, teachers, messages, portalNotifications);
    }
    @Test void unrelatedStudentCannotMessageTeacher() {
        UserEntity student = UserEntity.builder().id(1L).role(Role.STUDENT).email("student@test.local").build();
        UserEntity teacher = UserEntity.builder().id(2L).role(Role.TEACHER).email("teacher@test.local").build();
        when(security.getCurrentUser()).thenReturn(Optional.of(student));
        when(users.findById(2L)).thenReturn(Optional.of(teacher));
        when(teachers.findByEmailIgnoreCase(teacher.getEmail())).thenReturn(Optional.of(TeacherEntity.builder().id(2L).assignedClass("8").build()));
        when(students.findByEmailIgnoreCase(student.getEmail())).thenReturn(Optional.of(StudentEntity.builder().className("9").section("A").build()));
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> controller.send(2L, new ChatController.SendRequest("hello"))).getStatusCode().value());
        verifyNoInteractions(messages);
    }
    @Test void linkedStudentMessageUsesAuthenticatedSenderAndPersists() {
        UserEntity student = UserEntity.builder().id(1L).role(Role.STUDENT).email("student@test.local").build();
        UserEntity teacher = UserEntity.builder().id(2L).role(Role.TEACHER).email("teacher@test.local").build();
        when(security.getCurrentUser()).thenReturn(Optional.of(student));
        when(users.findById(2L)).thenReturn(Optional.of(teacher));
        when(teachers.findByEmailIgnoreCase(teacher.getEmail())).thenReturn(Optional.of(TeacherEntity.builder().id(2L).assignedClass("9 - A").build()));
        when(students.findByEmailIgnoreCase(student.getEmail())).thenReturn(Optional.of(StudentEntity.builder().className("9").section("A").build()));
        when(messages.save(any())).thenAnswer(invocation -> { ChatMessageEntity saved = invocation.getArgument(0); saved.setId(3L); return saved; });
        controller.send(2L, new ChatController.SendRequest("  Question about homework  "));
        ArgumentCaptor<ChatMessageEntity> saved = ArgumentCaptor.forClass(ChatMessageEntity.class);
        verify(messages).save(saved.capture());
        assertEquals(1L, saved.getValue().getSenderId());
        assertEquals(2L, saved.getValue().getRecipientId());
        assertEquals("Question about homework", saved.getValue().getContent());
        assertNotNull(saved.getValue().getSentAt());
    }
}

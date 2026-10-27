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
class GuardianFlowTest {
    @Mock SecurityUtil securityUtil;
    @Mock StudentRepository studentRepository;
    @Mock GuardianRepository guardianRepository;
    @InjectMocks MobileAppController controller;

    private void login(Role role) {
        when(securityUtil.getCurrentUser()).thenReturn(Optional.of(UserEntity.builder()
                .role(role).email("user@test.local").build()));
    }

    @Test void returnsEveryDashboardGuardianForAuthenticatedStudent() {
        login(Role.STUDENT);
        when(studentRepository.findByEmailIgnoreCase("user@test.local")).thenReturn(Optional.of(
                StudentEntity.builder().id(1L).admissionNo("ADM1").name("Student").build()));
        when(guardianRepository.findLinkedToStudent("ADM1")).thenReturn(List.of(
                GuardianEntity.builder().id(1L).name("Mother").studentAdmissionNo("ADM1").build(),
                GuardianEntity.builder().id(2L).name("Father").studentAdmissionNo("ADM1, ADM2").build(),
                GuardianEntity.builder().id(3L).name("Sister").studentAdmissionNo("ADM1").build()));
        var body = controller.getStudentGuardians(null).getBody();
        assertEquals(3, body.size());
        assertEquals("Student", body.get(0).getStudentName());
        assertEquals("Sister", body.get(2).getName());
    }

    @Test void studentCannotRequestAnotherStudentsGuardians() {
        login(Role.STUDENT);
        when(studentRepository.findByEmailIgnoreCase("user@test.local")).thenReturn(Optional.of(
                StudentEntity.builder().admissionNo("OWN").build()));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> controller.getStudentGuardians("OTHER")).getStatusCode().value());
        verifyNoInteractions(guardianRepository);
    }

    @Test void parentCannotRequestUnlinkedChild() {
        login(Role.PARENT);
        when(studentRepository.findByGuardianEmailIgnoreCase("user@test.local")).thenReturn(List.of());
        when(guardianRepository.findByEmailIgnoreCase("user@test.local")).thenReturn(Optional.empty());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> controller.getStudentGuardians("OTHER")).getStatusCode().value());
        verify(guardianRepository, never()).findLinkedToStudent(anyString());
    }
}

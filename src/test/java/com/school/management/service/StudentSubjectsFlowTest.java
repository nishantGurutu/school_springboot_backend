package com.school.management.service;

import com.school.management.controller.MobileAppController;
import com.school.management.domain.user.Role;
import com.school.management.domain.student.Gender;
import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class StudentSubjectsFlowTest {
    @Autowired MobileAppController mobile;
    @Autowired CurriculumService curriculum;
    @Autowired StudentRepository students;
    @Autowired SubjectRepository subjects;
    @MockBean SecurityUtil security;

    private StudentEntity student() {
        var student = students.save(StudentEntity.builder().admissionNo("SUB-FLOW-1").name("Flow Student")
                .firstName("Flow").lastName("Student").rollNo("1").className("Flow 7").section("A")
                .gender(Gender.MALE).dateOfBirth(LocalDate.of(2014, 1, 1))
                .phone("1234567890").email("subjects.flow@test.local").build());
        when(security.getCurrentUser()).thenReturn(Optional.of(UserEntity.builder()
                .role(Role.STUDENT).email(student.getEmail()).build()));
        return student;
    }

    private SubjectEntity subject(String name, String className, String status) {
        return subjects.save(SubjectEntity.builder().name(name).code("FLOW-" + name)
                .className(className).status(status).build());
    }

    @Test void studentSeesAllActiveClassSubjectsRegardlessOfOldSelectedSubjectIds() {
        var student = student();
        var math = subject("Mathematics", "Flow 7", "Active");
        var english = subject("English", "Flow 7", "Active");
        var other = subject("Physics", "Flow 8", "Active");
        subject("History", "Flow 7", "Inactive");
        subject("Global", null, "Active");
        student.setSubjectIds(other.getId().toString());
        students.saveAndFlush(student);
        var feed = mobile.getStudentSubjects(null).getBody();
        assertEquals(List.of(english.getId(), math.getId()), feed.stream().map(s -> s.getId()).toList());
        assertEquals("Flow 7", feed.get(0).getClassName());
        assertEquals(0, curriculum.getSubjectCurriculumForStudent(math.getId(), "Flow 8").get("totalChapters"));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> curriculum.getSubjectCurriculumForStudent(other.getId(), "Flow 8")).getStatusCode().value());
    }

    @Test void classChangeAndDashboardSubjectEditsImmediatelyChangeFeed() {
        var student = student();
        var math = subject("Mathematics", "Flow 7", "Active");
        var physics = subject("Physics", "Flow 8", "Active");
        assertEquals(math.getId(), mobile.getStudentSubjects(null).getBody().get(0).getId());
        student.setClassName("Flow 8");
        students.saveAndFlush(student);
        assertEquals(physics.getId(), mobile.getStudentSubjects(null).getBody().get(0).getId());
        subjects.delete(physics);
        subjects.flush();
        assertTrue(mobile.getStudentSubjects(null).getBody().isEmpty());
    }

    @Test void missingClassIsEmptyAndCannotRequestAnotherStudent() {
        var student = student();
        student.setClassName("");
        students.saveAndFlush(student);
        assertTrue(mobile.getStudentSubjects(null).getBody().isEmpty());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> mobile.getStudentSubjects("OTHER")).getStatusCode().value());
    }
}

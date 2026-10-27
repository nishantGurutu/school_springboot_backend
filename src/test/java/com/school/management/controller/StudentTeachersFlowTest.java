package com.school.management.controller;
import com.school.management.domain.user.Role;
import com.school.management.domain.student.Gender;
import com.school.management.domain.teacher.TeacherStatus;
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

@SpringBootTest @Transactional
class StudentTeachersFlowTest {
    @Autowired MobileAppController mobile;
    @Autowired TeacherRepository teachers;
    @Autowired StudentRepository students;
    @Autowired SchoolClassRepository classes;
    @Autowired SectionRepository sections;
    @MockBean SecurityUtil security;

    StudentEntity login() {
        var student=students.save(StudentEntity.builder().admissionNo("TEACHER-FLOW-1").email("teacherflow.student@test.local")
                .name("Student").firstName("Flow").lastName("Student").phone("1234567890").rollNo("1")
                .className("Teacher Flow 7").section("A").gender(Gender.MALE).dateOfBirth(LocalDate.of(2014,1,1)).build());
        when(security.getCurrentUser()).thenReturn(Optional.of(UserEntity.builder().role(Role.STUDENT).email(student.getEmail()).build()));
        return student;
    }
    SchoolClassEntity schoolClass(String name,String section) {
        return classes.save(SchoolClassEntity.builder().name(name).section(section).build());
    }
    TeacherEntity teacher(String name,SchoolClassEntity... assigned) {
        return teachers.save(TeacherEntity.builder().employeeId("TEACH-FLOW-"+name).firstName(name).lastName("Teacher")
                .email(name+".teacherflow@test.local").department("Science").subject("Science").phone("1234567899")
                .joiningDate(LocalDate.now()).assignedClasses(new HashSet<>(Arrays.asList(assigned))).build());
    }
    @Test void returnsAllAssignedTeachersOnceAndExcludesOtherClassSectionAndNonTeachers() {
        login();
        var own=schoolClass("Teacher Flow 7","A");
        var other=schoolClass("Teacher Flow 8","A");
        var b=schoolClass("Teacher Flow 7","B");
        var first=teacher("Alpha",own);
        var second=teacher("Beta",own,other);
        teacher("Other",other);teacher("SectionB",b);teacher("Unassigned");
        var retired=teacher("Retired",own);retired.setStatus(TeacherStatus.RETIRED);teachers.save(retired);
        var staff=teacher("Driver",own);staff.setType("Driver");teachers.save(staff);
        var result=mobile.getStudentTeachers(null).getBody();
        assertEquals(List.of(first.getId(),second.getId()),result.stream().map(t -> t.get("id")).toList());
        assertFalse(result.get(0).containsKey("bankAccountNumber"));
        assertEquals("Science",result.get(0).get("subject"));
    }
    @Test void mappingChangesAreReflectedWithoutCachedTeachers() {
        login();var own=schoolClass("Teacher Flow 7","A");var other=schoolClass("Teacher Flow 8","A");
        var teacher=teacher("Moved",own);teacher.setAssignedClass("Teacher Flow 7 (A)");teachers.save(teacher);
        assertEquals(1,mobile.getStudentTeachers(null).getBody().size());
        teacher.setAssignedClasses(new HashSet<>(Set.of(other)));teachers.saveAndFlush(teacher);
        assertTrue(mobile.getStudentTeachers(null).getBody().isEmpty());
    }
    @Test void legacyClassSummaryMatchesExactClassAndSection() {
        login();var teacher=teacher("Legacy");teacher.setAssignedClass("Teacher Flow 7 (A), Teacher Flow 8 (B)");teachers.save(teacher);
        var other=teacher("LegacyOther");other.setAssignedClass("Teacher Flow 7 (B)");teachers.save(other);
        assertEquals(List.of(teacher.getId()),mobile.getStudentTeachers(null).getBody().stream().map(t -> t.get("id")).toList());
    }
    @Test void cannotRequestAnotherStudentsTeachersAndMissingClassIsEmpty() {
        var student=login();
        assertEquals(403,assertThrows(ResponseStatusException.class,() -> mobile.getStudentTeachers("OTHER")).getStatusCode().value());
        student.setClassName("");students.saveAndFlush(student);
        assertTrue(mobile.getStudentTeachers(null).getBody().isEmpty());
    }
    @Test void sectionIdOnlyMappingsResolveTheSectionInsteadOfMatchingEverySection() {
        var student=login();student.setSection("Teacher Flow A");students.save(student);
        var a=sections.save(SectionEntity.builder().name("Teacher Flow A").build());
        var b=sections.save(SectionEntity.builder().name("Teacher Flow B").build());
        var own=classes.save(SchoolClassEntity.builder().name("Teacher Flow 7").sectionId(a.getId()).build());
        var other=classes.save(SchoolClassEntity.builder().name("Teacher Flow 7").sectionId(b.getId()).build());
        var assigned=teacher("SectionIdOwn",own);teacher("SectionIdOther",other);
        assertEquals(List.of(assigned.getId()),mobile.getStudentTeachers(null).getBody().stream().map(t -> t.get("id")).toList());
    }
}

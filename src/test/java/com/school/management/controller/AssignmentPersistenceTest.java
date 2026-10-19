package com.school.management.controller;

import com.school.management.domain.user.Role;
import com.school.management.domain.student.Gender;
import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class AssignmentPersistenceTest {
    @Autowired MobileAppController mobile;
    @Autowired HomeworkController dashboard;
    @Autowired StudentRepository students;
    @Autowired TeacherRepository teachers;
    @Autowired SchoolClassRepository classes;
    @Autowired HomeworkSubmissionRepository submissions;
    @Autowired EntityManager entityManager;
    @Autowired ExamResultRepository results;
    @MockBean SecurityUtil security;

    private void login(Role role, String email) {
        when(security.getCurrentUser()).thenReturn(Optional.of(UserEntity.builder().role(role).email(email).name("Test user").build()));
    }
    @Test void dashboardAndMobileSharePersistedAssignmentSubmissionAndReview() {
        SchoolClassEntity schoolClass = classes.save(SchoolClassEntity.builder().name("Integration 7").section("B").build());
        TeacherEntity teacher = teachers.save(TeacherEntity.builder().employeeId("FLOW-T1").firstName("Flow").lastName("Teacher")
            .department("Math").subject("Math").phone("1000000000").email("flow.teacher@test.local").joiningDate(LocalDate.now())
            .assignedClasses(new HashSet<>(Set.of(schoolClass))).build());
        StudentEntity child = students.save(StudentEntity.builder().admissionNo("FLOW-S1").name("Flow Student").firstName("Flow").lastName("Student")
            .rollNo("1").className(schoolClass.getName()).section("B").gender(Gender.MALE).dateOfBirth(LocalDate.of(2014,1,1))
            .phone("1000000001").email("flow.student@test.local").guardianEmail("flow.parent@test.local").build());
        login(Role.TEACHER, teacher.getEmail());
        Map<String,Object> created = dashboard.create(Map.of("title", "Persisted exercise", "subject", "Math", "className", schoolClass.getName(),
            "section", "B", "dueDate", LocalDate.now().plusDays(2).toString(), "points", 25)).getBody();
        String id = created.get("id").toString();
        entityManager.flush(); entityManager.clear();
        assertEquals(id, mobile.getTeacherAssignments(null,null).getBody().get(0).get("id"));
        login(Role.STUDENT, child.getEmail());
        assertEquals(id, mobile.getStudentAssignments(null).getBody().get(0).get("id"));
        mobile.submitStudentAssignment(id, Map.of("submissionText", "My answer", "fileUrl", "https://example.test/work.pdf"));
        entityManager.flush(); entityManager.clear();
        Long submissionId = submissions.findByHomeworkIdAndStudentId(((Number)created.get("dbId")).longValue(), child.getId()).orElseThrow().getId();
        login(Role.TEACHER, teacher.getEmail());
        mobile.reviewSubmission(id, submissionId, Map.of("status", "GRADED", "points", 23, "feedback", "Good work"));
        entityManager.flush(); entityManager.clear();
        login(Role.PARENT, "flow.parent@test.local");
        Map<String,Object> parentView = mobile.getStudentAssignments(child.getAdmissionNo()).getBody().get(0);
        assertEquals("graded", parentView.get("status"));
        assertEquals(23, parentView.get("scoredPoints"));
        assertEquals("Good work", parentView.get("teacherFeedback"));
        assertEquals("My answer", parentView.get("submissionText"));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> mobile.submitStudentAssignment(id, Map.of()));
        login(Role.TEACHER, teacher.getEmail());
        assertEquals(1, dashboard.list().getBody().get(0).get("submissionCount"));
        assertEquals(1L, dashboard.list().getBody().get(0).get("totalStudents"));
        Map<String,Object> update = new HashMap<>(Map.of("title", "Updated instructions", "subject", "Math", "className", schoolClass.getName(), "section", "B", "dueDate", LocalDate.now().plusDays(2).toString(), "points", 25));
        assertEquals(id, mobile.updateTeacherAssignment(id, update).getBody().get("id"));
        entityManager.flush(); entityManager.clear();
        login(Role.PARENT, "flow.parent@test.local");
        assertEquals("Updated instructions", mobile.getStudentAssignments(child.getAdmissionNo()).getBody().get(0).get("title"));
        login(Role.TEACHER, teacher.getEmail());
        mobile.deleteTeacherAssignment(id);
        entityManager.flush(); entityManager.clear();
        assertTrue(submissions.findByHomeworkIdOrderByIdDesc(((Number)created.get("dbId")).longValue()).isEmpty());
        login(Role.PARENT, "flow.parent@test.local");
        assertTrue(mobile.getStudentAssignments(child.getAdmissionNo()).getBody().isEmpty());
        login(Role.TEACHER, teacher.getEmail());
        Map<String,Object> exam = mobile.createExam(Map.of("title", "Persisted math exam", "subject", "Math", "className", schoolClass.getName(),
            "section", "B", "date", LocalDate.now().plusDays(3).toString(), "startTime", "09:00", "endTime", "10:00", "maxMarks", 25)).getBody();
        String examId = exam.get("id").toString();
        mobile.uploadExamScore(examId, Map.of("admissionNo", child.getAdmissionNo(), "scoredMarks", 20));
        mobile.uploadExamScore(examId, Map.of("admissionNo", child.getAdmissionNo(), "scoredMarks", 22));
        entityManager.flush(); entityManager.clear();
        assertEquals(1, results.findByAdmissionNoIgnoreCaseOrderByIdDesc(child.getAdmissionNo()).size());
        assertEquals(88, results.findByAdmissionNoIgnoreCaseOrderByIdDesc(child.getAdmissionNo()).get(0).getPercent());
        login(Role.PARENT, "flow.parent@test.local");
        Map<String,Object> result = mobile.getExams(child.getAdmissionNo()).getBody().stream().filter(row -> "completed".equals(row.get("status"))).findFirst().orElseThrow();
        assertEquals(25.0, result.get("maxMarks"));
        assertEquals(22.0, result.get("scoredMarks"));
        login(Role.TEACHER, teacher.getEmail());
        mobile.markAttendance(Map.of("attendanceType", "STUDENT", "admissionNo", child.getAdmissionNo(), "date", LocalDate.now().toString(), "status", "absent"));
        mobile.markAttendance(Map.of("attendanceType", "STUDENT", "admissionNo", child.getAdmissionNo(), "date", LocalDate.now().toString(), "status", "present"));
        entityManager.flush(); entityManager.clear();
        login(Role.PARENT, "flow.parent@test.local");
        var attendance = mobile.getAttendance(null, null, null, child.getAdmissionNo()).getBody();
        assertEquals(1, attendance.size());
        assertEquals("present", attendance.get(0).get("status"));
        assertEquals(child.getName(), attendance.get(0).get("name"));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> mobile.markAttendance(Map.of("attendanceType", "STUDENT", "admissionNo", child.getAdmissionNo())));
    }
}

package com.school.management.service;

import com.school.management.dto.*;
import com.school.management.entity.*;
import com.school.management.exceptions.BadRequestException;
import com.school.management.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ExaminationClassFlowTest {
    @Autowired ExamService exams;
    @Autowired ExamScheduleService schedules;
    @Autowired SubjectService subjects;
    @Autowired SchoolClassRepository classes;
    @Autowired SectionRepository sections;
    @Autowired ExamScheduleRepository scheduleRepository;
    @Autowired TeacherRepository teacherRepository;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private String examName = "Flow Annual Exam";

    @BeforeEach void setup() {
        var a = sections.save(SectionEntity.builder().name("FLOW-A").build());
        classes.save(SchoolClassEntity.builder().name("Flow 7").sectionId(a.getId()).section(a.getName()).build());
        classes.save(SchoolClassEntity.builder().name("Flow 8").section("FLOW-B").build());
        var exam = new ExamRequest();
        exam.setName(examName);
        exam.setStatus("Active");
        exams.create(exam);
        var subject = new SubjectRequest();
        subject.setName("Flow Mathematics");
        subject.setCode("FLOW-MATH-7");
        subject.setClassName("Flow 7");
        subjects.create(subject);
    }

    private ExamScheduleRequest request() {
        var request = new ExamScheduleRequest();
        request.setExamName(examName);
        request.setClassName("Flow 7");
        request.setSection("FLOW-A");
        request.setSubject("Flow Mathematics");
        request.setDate("2026-11-20");
        request.setStartTime("09:00 AM");
        request.setEndTime("12:00 PM");
        return request;
    }

    @Test void examHasNoTimingAndScheduleRetainsClassAndTiming() {
        var exam = exams.getAll().stream().filter(e -> examName.equals(e.getName())).findFirst().orElseThrow();
        assertNull(exam.getDate());
        assertNull(exam.getStartTime());
        assertNull(exam.getEndTime());
        var schedule = schedules.create(request());
        var persisted = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertEquals("Flow 7", persisted.getClassName());
        assertEquals("FLOW-A", persisted.getSection());
        assertEquals("Flow Mathematics", persisted.getSubject());
        assertEquals("2026-11-20", persisted.getDate());
        assertEquals("Flow 7", subjects.getAll().stream().filter(s -> s.getCode().equals("FLOW-MATH-7")).findFirst().orElseThrow().getClassName());
    }

    @Test void rejectsSectionOrSubjectFromAnotherClassOnCreateAndEdit() {
        var request = request();
        request.setSection("FLOW-B");
        assertThrows(BadRequestException.class, () -> schedules.create(request));
        request.setSection("FLOW-A");
        var saved = schedules.create(request);
        request.setClassName("Flow 8");
        request.setSection("FLOW-B");
        assertThrows(BadRequestException.class, () -> schedules.update(saved.getId(), request));
        assertEquals("Flow 7", scheduleRepository.findById(saved.getId()).orElseThrow().getClassName());
    }

    @Test void cannotUseExamNameAsClassOrAnUnknownExam() {
        var request = request();
        request.setClassName(null);
        assertThrows(BadRequestException.class, () -> schedules.create(request));
        request.setClassName("Flow 7");
        request.setExamName("Unknown exam");
        assertThrows(BadRequestException.class, () -> schedules.create(request));
    }

    @Test void subjectClassAssignmentCanBeUpdatedAndReturned() {
        var subject = subjects.getAll().stream().filter(s -> s.getCode().equals("FLOW-MATH-7")).findFirst().orElseThrow();
        var request = new SubjectRequest();
        request.setName(subject.getName());
        request.setCode(subject.getCode());
        request.setClassName("Flow 8");
        assertEquals("Flow 8", subjects.update(subject.getId(), request).getClassName());
        assertThrows(BadRequestException.class, () -> schedules.create(request()));
    }

    @Test void deleteSubjectRemovesAssociationsAndSucceeds() {
        var subject = subjects.getAll().stream().filter(s -> s.getCode().equals("FLOW-MATH-7")).findFirst().orElseThrow();
        var teacher = teacherRepository.save(TeacherEntity.builder()
                .employeeId("T-TEST-DEL")
                .firstName("Test")
                .lastName("Teacher")
                .phone("9999999999")
                .email("test.teacher@test.com")
                .department("Academics")
                .joiningDate(java.time.LocalDate.now())
                .subject("Flow Mathematics")
                .subjectIds(String.valueOf(subject.getId()))
                .build());
        jdbcTemplate.update("INSERT INTO teacher_subjects (teacher_id, subject_id) VALUES (?, ?)", teacher.getId(), subject.getId());

        assertDoesNotThrow(() -> subjects.delete(subject.getId()));
        assertTrue(subjects.getAll().stream().noneMatch(s -> s.getId().equals(subject.getId())));
    }
}

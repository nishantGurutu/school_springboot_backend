package com.school.management.controller;

import com.school.management.domain.student.Gender;
import com.school.management.domain.student.StudentStatus;
import com.school.management.domain.teacher.TeacherStatus;
import com.school.management.entity.StudentEntity;
import com.school.management.repository.StudentRepository;
import com.school.management.response.ApiResponse;
import com.school.management.service.StudentService;
import com.school.management.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Dashboard statistics for school admins")
public class AdminDashboardController {

    private final StudentService studentService;
    private final TeacherService teacherService;
    private final StudentRepository studentRepository;

    @Getter
    public static class DashboardStats {
        private final long totalStudents;
        private final long activeStudents;
        private final long totalTeachers;
        private final long activeTeachers;
        private final long boys;
        private final long girls;
        private final long otherStudents;

        DashboardStats(long totalStudents, long activeStudents, long totalTeachers, long activeTeachers,
                       long boys, long girls, long otherStudents) {
            this.totalStudents = totalStudents;
            this.activeStudents = activeStudents;
            this.totalTeachers = totalTeachers;
            this.activeTeachers = activeTeachers;
            this.boys = boys;
            this.girls = girls;
            this.otherStudents = otherStudents;
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics")
    public ResponseEntity<ApiResponse<DashboardStats>> getStats() {
        long totalStudents = studentService.countByStatus(StudentStatus.ACTIVE)
                + studentService.countByStatus(StudentStatus.INACTIVE)
                + studentService.countByStatus(StudentStatus.TRANSFERRED)
                + studentService.countByStatus(StudentStatus.WITHDRAWN)
                + studentService.countByStatus(StudentStatus.GRADUATED);
        long activeStudents = studentService.countByStatus(StudentStatus.ACTIVE);
        long totalTeachers = teacherService.countByStatus(TeacherStatus.ACTIVE)
                + teacherService.countByStatus(TeacherStatus.ON_LEAVE)
                + teacherService.countByStatus(TeacherStatus.RESIGNED)
                + teacherService.countByStatus(TeacherStatus.RETIRED);
        long activeTeachers = teacherService.countByStatus(TeacherStatus.ACTIVE);

        List<StudentEntity> allStudents = studentRepository.findAll();
        long boys = allStudents.stream().filter(s -> s.getGender() == Gender.MALE).count();
        long girls = allStudents.stream().filter(s -> s.getGender() == Gender.FEMALE).count();
        long otherStudents = allStudents.size() - boys - girls;

        DashboardStats stats = new DashboardStats(
                totalStudents, activeStudents, totalTeachers, activeTeachers, boys, girls, otherStudents);
        return ResponseEntity.ok(ApiResponse.ok("Dashboard stats", stats));
    }
}
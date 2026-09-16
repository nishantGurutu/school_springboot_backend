package com.school.management.service;

import com.school.management.entity.TeacherEntity;
import com.school.management.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HrmService {

    private final TeacherRepository teacherRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getSummary() {
        List<TeacherEntity> teachers = teacherRepository.findAll();

        long totalTeachers = teachers.size();
        long facultyStaff = teachers.stream()
                .filter(t -> t.getDesignation() == null
                        || t.getDesignation().toLowerCase(Locale.ROOT).contains("teacher"))
                .count();
        long administrativeStaff = teachers.stream()
                .filter(t -> t.getDesignation() != null
                        && (t.getDesignation().toLowerCase(Locale.ROOT).contains("admin")
                        || t.getDesignation().toLowerCase(Locale.ROOT).contains("principal")))
                .count();
        long supportStaff = Math.max(totalTeachers - facultyStaff - administrativeStaff, 0);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalTeachers", totalTeachers);
        summary.put("facultyStaff", facultyStaff);
        summary.put("administrativeStaff", administrativeStaff);
        summary.put("supportStaff", supportStaff);
        return summary;
    }
}
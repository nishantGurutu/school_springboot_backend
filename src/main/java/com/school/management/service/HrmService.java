package com.school.management.service;

import com.school.management.entity.StaffEntity;
import com.school.management.entity.TeacherEntity;
import com.school.management.repository.StaffRepository;
import com.school.management.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HrmService {

    private final TeacherRepository teacherRepository;
    private final StaffRepository staffRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getSummary() {
        List<TeacherEntity> teachers = teacherRepository.findAll();
        List<StaffEntity> staffMembers = staffRepository.findAll();

        long totalTeachers = teachers.size();
        long facultyStaff = totalTeachers;

        long administrativeStaff = staffMembers.stream()
                .filter(s -> s.getStaffType() != null && s.getStaffType().equalsIgnoreCase("Accounts"))
                .count();

        long supportStaff = staffMembers.stream()
                .filter(s -> s.getStaffType() != null && !s.getStaffType().equalsIgnoreCase("Accounts"))
                .count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalTeachers", totalTeachers);
        summary.put("facultyStaff", facultyStaff);
        summary.put("administrativeStaff", administrativeStaff);
        summary.put("supportStaff", supportStaff);
        return summary;
    }
}
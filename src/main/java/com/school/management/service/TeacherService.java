package com.school.management.service;

import com.school.management.domain.teacher.TeacherStatus;
import com.school.management.dto.TeacherRequest;
import com.school.management.dto.TeacherResponse;
import org.springframework.data.domain.Page;

public interface TeacherService {

    TeacherResponse create(TeacherRequest request);

    TeacherResponse getById(Long id);

    TeacherResponse getByEmployeeId(String employeeId);

    TeacherResponse getByEmail(String email);

    Page<TeacherResponse> getAll(int page, int size, String sortBy, String sortDir);

    Page<TeacherResponse> search(String query, int page, int size);

    Page<TeacherResponse> getByDepartment(String department, int page, int size);

    TeacherResponse update(Long id, TeacherRequest request);

    TeacherResponse updateStatus(Long id, String status);

    void delete(Long id);

    long countByStatus(TeacherStatus status);
}
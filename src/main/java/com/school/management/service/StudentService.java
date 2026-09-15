package com.school.management.service;

import com.school.management.domain.student.StudentStatus;
import com.school.management.dto.StudentRequest;
import com.school.management.dto.StudentResponse;
import org.springframework.data.domain.Page;

public interface StudentService {

    StudentResponse create(StudentRequest request);

    StudentResponse getById(Long id);

    StudentResponse getByAdmissionNo(String admissionNo);

    Page<StudentResponse> getAll(int page, int size, String sortBy, String sortDir);

    Page<StudentResponse> search(String query, int page, int size);

    Page<StudentResponse> getByClass(String className, int page, int size);

    StudentResponse update(Long id, StudentRequest request);

    StudentResponse updateStatus(Long id, String status);

    void delete(Long id);

    long countByStatus(StudentStatus status);
}
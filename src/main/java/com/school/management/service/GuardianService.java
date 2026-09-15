package com.school.management.service;

import com.school.management.dto.GuardianRequest;
import com.school.management.dto.GuardianResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface GuardianService {

    GuardianResponse create(GuardianRequest request);

    GuardianResponse getById(Long id);

    GuardianResponse getByEmail(String email);

    List<GuardianResponse> getByStudent(String studentAdmissionNo);

    Page<GuardianResponse> getAll(int page, int size, String sortBy, String sortDir);

    Page<GuardianResponse> search(String query, int page, int size);

    GuardianResponse update(Long id, GuardianRequest request);

    void delete(Long id);
}
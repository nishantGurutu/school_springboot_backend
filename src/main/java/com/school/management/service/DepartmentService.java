package com.school.management.service;

import com.school.management.dto.DepartmentRequest;
import com.school.management.dto.DepartmentResponse;
import com.school.management.entity.DepartmentEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAll() {
        return departmentRepository.findAllByOrderByIdAsc().stream()
                .map(DepartmentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getById(Long id) {
        return DepartmentResponse.fromEntity(findByIdOrThrow(id));
    }

    public DepartmentResponse create(DepartmentRequest request) {
        String name = requireNonBlank(request.getName(), "Department name is required").trim();
        String code = requireNonBlank(request.getCode(), "Department code is required").trim().toUpperCase();

        if (departmentRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Department with code '" + code + "' already exists");
        }
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Department with name '" + name + "' already exists");
        }

        String status = (request.getStatus() == null || request.getStatus().isBlank()) ? "Active" : request.getStatus().trim();

        DepartmentEntity entity = DepartmentEntity.builder()
                .name(name)
                .code(code)
                .headOfDepartment(request.getHeadOfDepartment() != null ? request.getHeadOfDepartment().trim() : "")
                .description(request.getDescription() != null ? request.getDescription().trim() : "")
                .status(status)
                .build();

        return DepartmentResponse.fromEntity(departmentRepository.save(entity));
    }

    public DepartmentResponse update(Long id, DepartmentRequest request) {
        DepartmentEntity entity = findByIdOrThrow(id);
        String name = requireNonBlank(request.getName(), "Department name is required").trim();
        String code = requireNonBlank(request.getCode(), "Department code is required").trim().toUpperCase();

        if (!entity.getCode().equalsIgnoreCase(code) && departmentRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Department with code '" + code + "' already exists");
        }
        if (!entity.getName().equalsIgnoreCase(name) && departmentRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Department with name '" + name + "' already exists");
        }

        entity.setName(name);
        entity.setCode(code);
        entity.setHeadOfDepartment(request.getHeadOfDepartment() != null ? request.getHeadOfDepartment().trim() : "");
        entity.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            entity.setStatus(request.getStatus().trim());
        }

        return DepartmentResponse.fromEntity(departmentRepository.save(entity));
    }

    public void delete(Long id) {
        DepartmentEntity entity = findByIdOrThrow(id);
        departmentRepository.delete(entity);
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private DepartmentEntity findByIdOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }
}

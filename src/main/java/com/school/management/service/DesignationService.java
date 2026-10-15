package com.school.management.service;

import com.school.management.dto.DesignationRequest;
import com.school.management.dto.DesignationResponse;
import com.school.management.entity.DepartmentEntity;
import com.school.management.entity.DesignationEntity;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.DepartmentRepository;
import com.school.management.repository.DesignationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DesignationService {

    private final DesignationRepository designationRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public List<DesignationResponse> getAll() {
        return designationRepository.findAllByOrderByIdAsc().stream()
                .map(DesignationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DesignationResponse> getByDepartmentId(Long departmentId) {
        return designationRepository.findByDepartmentIdOrderByIdAsc(departmentId).stream()
                .map(DesignationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public DesignationResponse getById(Long id) {
        DesignationEntity entity = designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found with id: " + id));
        return DesignationResponse.fromEntity(entity);
    }

    @Transactional
    public DesignationResponse create(DesignationRequest request) {
        String trimmedName = request.getName().trim();

        DepartmentEntity department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

            if (designationRepository.existsByDepartmentIdAndNameIgnoreCase(department.getId(), trimmedName)) {
                throw new DuplicateResourceException("Designation '" + trimmedName + "' already exists in " + department.getName() + " department");
            }
        } else {
            if (designationRepository.existsByNameIgnoreCase(trimmedName)) {
                throw new DuplicateResourceException("Designation already exists with name: " + trimmedName);
            }
        }

        String code = request.getCode() != null && !request.getCode().isBlank()
                ? request.getCode().trim().toUpperCase()
                : generateCodeFromName(trimmedName);

        if (department != null && code != null && !code.isBlank()) {
            if (designationRepository.existsByDepartmentIdAndCodeIgnoreCase(department.getId(), code)) {
                throw new DuplicateResourceException("Designation code '" + code + "' already in use in " + department.getName() + " department");
            }
        }

        DesignationEntity entity = DesignationEntity.builder()
                .name(trimmedName)
                .code(code)
                .department(department)
                .category(request.getCategory() != null && !request.getCategory().isBlank()
                        ? request.getCategory().trim()
                        : "")
                .description(request.getDescription() != null ? request.getDescription().trim() : "")
                .status(request.getStatus() != null && !request.getStatus().isBlank()
                        ? request.getStatus().trim()
                        : "Active")
                .build();

        DesignationEntity saved = designationRepository.save(entity);
        log.info("Created designation: {} under department: {} (ID: {})",
                saved.getName(),
                saved.getDepartment() != null ? saved.getDepartment().getName() : "None",
                saved.getId());
        return DesignationResponse.fromEntity(saved);
    }

    @Transactional
    public DesignationResponse update(Long id, DesignationRequest request) {
        DesignationEntity entity = designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found with id: " + id));

        String trimmedName = request.getName().trim();

        DepartmentEntity department = entity.getDepartment();
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));
        }

        if (department != null) {
            if (designationRepository.existsByDepartmentIdAndNameIgnoreCaseAndIdNot(department.getId(), trimmedName, id)) {
                throw new DuplicateResourceException("Another designation with name '" + trimmedName + "' already exists in " + department.getName() + " department");
            }
        } else {
            if (designationRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
                throw new DuplicateResourceException("Another designation already exists with name: " + trimmedName);
            }
        }

        String code = request.getCode() != null && !request.getCode().isBlank()
                ? request.getCode().trim().toUpperCase()
                : entity.getCode();

        if (department != null && code != null && !code.isBlank()) {
            if (designationRepository.existsByDepartmentIdAndCodeIgnoreCaseAndIdNot(department.getId(), code, id)) {
                throw new DuplicateResourceException("Another designation in " + department.getName() + " already uses code: " + code);
            }
        }

        entity.setName(trimmedName);
        entity.setCode(code);
        entity.setDepartment(department);
        if (request.getCategory() != null) {
            entity.setCategory(request.getCategory().trim());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription().trim());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            entity.setStatus(request.getStatus().trim());
        }

        DesignationEntity updated = designationRepository.save(entity);
        log.info("Updated designation: {} under department: {} (ID: {})",
                updated.getName(),
                updated.getDepartment() != null ? updated.getDepartment().getName() : "None",
                updated.getId());
        return DesignationResponse.fromEntity(updated);
    }

    @Transactional
    public void delete(Long id) {
        if (!designationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Designation not found with id: " + id);
        }
        designationRepository.deleteById(id);
        log.info("Deleted designation with ID: {}", id);
    }

    private String generateCodeFromName(String name) {
        if (name == null || name.isBlank()) return "DSG";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].length() <= 4 ? parts[0].toUpperCase() : parts[0].substring(0, 4).toUpperCase();
        }
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isBlank()) sb.append(p.charAt(0));
        }
        return sb.toString().toUpperCase();
    }
}

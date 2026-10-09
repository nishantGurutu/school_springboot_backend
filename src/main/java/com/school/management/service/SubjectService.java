package com.school.management.service;

import com.school.management.dto.SubjectRequest;
import com.school.management.dto.SubjectResponse;
import com.school.management.entity.SubjectEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubjectService {

    private final SubjectRepository subjectRepository;

    @Transactional(readOnly = true)
    public List<SubjectResponse> getAll() {
        return subjectRepository.findAll().stream()
                .map(SubjectResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubjectResponse getById(Long id) {
        return SubjectResponse.fromEntity(findByIdOrThrow(id));
    }

    public SubjectResponse create(SubjectRequest request) {
        String name = requireNonBlank(request.getName(), "Name is required").trim();
        String code = requireNonBlank(request.getCode(), "Code is required").trim();
        if (subjectRepository.existsByCode(code)) {
            throw new DuplicateResourceException(
                    "Subject with code '" + code + "' already exists");
        }
        String status = (request.getStatus() == null || request.getStatus().isBlank()) ? "Active" : request.getStatus().trim();
        SubjectEntity entity = SubjectEntity.builder()
                .name(name)
                .code(code)
                .status(status)
                .build();
        return SubjectResponse.fromEntity(subjectRepository.save(entity));
    }

    public SubjectResponse update(Long id, SubjectRequest request) {
        SubjectEntity entity = findByIdOrThrow(id);
        String name = requireNonBlank(request.getName(), "Name is required").trim();
        String code = requireNonBlank(request.getCode(), "Code is required").trim();
        if (!entity.getCode().equalsIgnoreCase(code) && subjectRepository.existsByCode(code)) {
            throw new DuplicateResourceException(
                    "Subject with code '" + code + "' already exists");
        }
        entity.setName(name);
        entity.setCode(code);
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            entity.setStatus(request.getStatus().trim());
        }
        return SubjectResponse.fromEntity(subjectRepository.save(entity));
    }

    public void delete(Long id) {
        SubjectEntity entity = findByIdOrThrow(id);
        subjectRepository.delete(entity);
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private SubjectEntity findByIdOrThrow(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));
    }
}

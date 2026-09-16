package com.school.management.service;

import com.school.management.dto.SubjectRequest;
import com.school.management.dto.SubjectResponse;
import com.school.management.entity.SubjectEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SubjectService {

    private final SubjectRepository subjectRepository;

    @Transactional(readOnly = true)
    public List<SubjectResponse> getAll() {
        return subjectRepository.findAll().stream()
                .map(SubjectResponse::fromEntity)
                .toList();
    }

    public SubjectResponse create(SubjectRequest request) {
        String name = requireNonBlank(request.getName(), "Name is required");
        String code = requireNonBlank(request.getCode(), "Code is required");
        if (subjectRepository.existsByCode(code.trim())) {
            throw new DuplicateResourceException(
                    "Subject with code '" + code + "' already exists");
        }
        SubjectEntity entity = SubjectEntity.builder()
                .name(name.trim())
                .code(code.trim())
                .build();
        return SubjectResponse.fromEntity(subjectRepository.save(entity));
    }

    public SubjectResponse update(Long id, SubjectRequest request) {
        SubjectEntity entity = findByIdOrThrow(id);
        String name = requireNonBlank(request.getName(), "Name is required");
        String code = requireNonBlank(request.getCode(), "Code is required");
        if (!entity.getCode().equals(code.trim()) && subjectRepository.existsByCode(code.trim())) {
            throw new DuplicateResourceException(
                    "Subject with code '" + code + "' already exists");
        }
        entity.setName(name.trim());
        entity.setCode(code.trim());
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

package com.school.management.service;

import com.school.management.dto.SchoolClassRequest;
import com.school.management.dto.SchoolClassResponse;
import com.school.management.entity.SchoolClassEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.SchoolClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;

    @Transactional(readOnly = true)
    public List<SchoolClassResponse> getAll() {
        return schoolClassRepository.findAll().stream()
                .map(SchoolClassResponse::fromEntity)
                .toList();
    }

    public SchoolClassResponse create(SchoolClassRequest request) {
        String name = requireNonBlank(request.getName(), "Name is required");
        if (schoolClassRepository.existsByName(name.trim())) {
            throw new DuplicateResourceException(
                    "Class with name '" + name + "' already exists");
        }
        SchoolClassEntity entity = SchoolClassEntity.builder()
                .name(name.trim())
                .section(request.getSection() == null ? null : request.getSection().trim())
                .build();
        return SchoolClassResponse.fromEntity(schoolClassRepository.save(entity));
    }

    public SchoolClassResponse update(Long id, SchoolClassRequest request) {
        SchoolClassEntity entity = findByIdOrThrow(id);
        String name = requireNonBlank(request.getName(), "Name is required");
        if (!entity.getName().equals(name.trim()) && schoolClassRepository.existsByName(name.trim())) {
            throw new DuplicateResourceException(
                    "Class with name '" + name + "' already exists");
        }
        entity.setName(name.trim());
        entity.setSection(request.getSection() == null ? null : request.getSection().trim());
        return SchoolClassResponse.fromEntity(schoolClassRepository.save(entity));
    }

    public void delete(Long id) {
        SchoolClassEntity entity = findByIdOrThrow(id);
        schoolClassRepository.delete(entity);
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private SchoolClassEntity findByIdOrThrow(Long id) {
        return schoolClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));
    }
}

package com.school.management.service;

import com.school.management.dto.SchoolClassRequest;
import com.school.management.dto.SchoolClassResponse;
import com.school.management.entity.SchoolClassEntity;
import com.school.management.entity.SectionEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.SchoolClassRepository;
import com.school.management.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final SectionRepository sectionRepository;

    @Transactional(readOnly = true)
    public List<SchoolClassResponse> getAll() {
        return schoolClassRepository.findAll().stream()
                .map(SchoolClassResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public SchoolClassResponse getById(Long id) {
        return SchoolClassResponse.fromEntity(findByIdOrThrow(id));
    }

    public SchoolClassResponse create(SchoolClassRequest request) {
        String name = requireNonBlank(request.getName(), "Name is required").trim();
        Long sectionId = request.getSectionId();
        String sectionName = request.getSection() != null ? request.getSection().trim() : null;

        if (sectionId != null) {
            SectionEntity sec = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + sectionId));
            sectionName = sec.getName();
            if (schoolClassRepository.existsByNameAndSectionId(name, sectionId)) {
                throw new DuplicateResourceException(
                        "Class '" + name + "' with Section '" + sectionName + "' already exists");
            }
        } else if (sectionName != null && !sectionName.isBlank()) {
            if (schoolClassRepository.existsByNameAndSection(name, sectionName)) {
                throw new DuplicateResourceException(
                        "Class '" + name + "' with Section '" + sectionName + "' already exists");
            }
        } else {
            if (schoolClassRepository.existsByName(name)) {
                throw new DuplicateResourceException(
                        "Class with name '" + name + "' already exists");
            }
        }

        String status = (request.getStatus() == null || request.getStatus().isBlank()) ? "Active" : request.getStatus().trim();

        SchoolClassEntity entity = SchoolClassEntity.builder()
                .name(name)
                .sectionId(sectionId)
                .section(sectionName)
                .status(status)
                .build();
        return SchoolClassResponse.fromEntity(schoolClassRepository.save(entity));
    }

    public SchoolClassResponse update(Long id, SchoolClassRequest request) {
        SchoolClassEntity entity = findByIdOrThrow(id);
        String name = requireNonBlank(request.getName(), "Name is required").trim();
        Long sectionId = request.getSectionId();
        String sectionName = request.getSection() != null ? request.getSection().trim() : null;

        if (sectionId != null) {
            SectionEntity sec = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + sectionId));
            sectionName = sec.getName();
            var existing = schoolClassRepository.findByNameAndSectionId(name, sectionId);
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new DuplicateResourceException(
                        "Class '" + name + "' with Section '" + sectionName + "' already exists");
            }
        } else if (sectionName != null && !sectionName.isBlank()) {
            var existing = schoolClassRepository.findByNameAndSection(name, sectionName);
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new DuplicateResourceException(
                        "Class '" + name + "' with Section '" + sectionName + "' already exists");
            }
        } else {
            if (!entity.getName().equalsIgnoreCase(name) && schoolClassRepository.existsByName(name)) {
                throw new DuplicateResourceException(
                        "Class with name '" + name + "' already exists");
            }
        }

        entity.setName(name);
        entity.setSectionId(sectionId);
        entity.setSection(sectionName);
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            entity.setStatus(request.getStatus().trim());
        }

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

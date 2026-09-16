package com.school.management.service;

import com.school.management.dto.SectionRequest;
import com.school.management.dto.SectionResponse;
import com.school.management.entity.SectionEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SectionService {

    private final SectionRepository sectionRepository;

    @Transactional(readOnly = true)
    public List<SectionResponse> getAll() {
        return sectionRepository.findAll().stream()
                .map(SectionResponse::fromEntity)
                .toList();
    }

    public SectionResponse create(SectionRequest request) {
        String name = requireNonBlank(request.getName(), "Name is required");
        if (sectionRepository.existsByName(name.trim())) {
            throw new DuplicateResourceException(
                    "Section with name '" + name + "' already exists");
        }
        SectionEntity entity = SectionEntity.builder()
                .name(name.trim())
                .build();
        return SectionResponse.fromEntity(sectionRepository.save(entity));
    }

    public SectionResponse update(Long id, SectionRequest request) {
        SectionEntity entity = findByIdOrThrow(id);
        String name = requireNonBlank(request.getName(), "Name is required");
        if (!entity.getName().equals(name.trim()) && sectionRepository.existsByName(name.trim())) {
            throw new DuplicateResourceException(
                    "Section with name '" + name + "' already exists");
        }
        entity.setName(name.trim());
        return SectionResponse.fromEntity(sectionRepository.save(entity));
    }

    public void delete(Long id) {
        SectionEntity entity = findByIdOrThrow(id);
        sectionRepository.delete(entity);
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private SectionEntity findByIdOrThrow(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id: " + id));
    }
}

package com.school.management.service;

import com.school.management.dto.SectionRequest;
import com.school.management.dto.SectionResponse;
import com.school.management.entity.SectionEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.SectionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SectionService {

    private final SectionRepository sectionRepository;

    @PostConstruct
    public void seedInitialSectionsIfEmpty() {
        try {
            if (sectionRepository.count() == 0) {
                log.info("Seeding initial sections A, B, C, D...");
                List<SectionEntity> initialSections = List.of(
                        SectionEntity.builder().name("A").status("Active").build(),
                        SectionEntity.builder().name("B").status("Active").build(),
                        SectionEntity.builder().name("C").status("Active").build(),
                        SectionEntity.builder().name("D").status("Active").build()
                );
                sectionRepository.saveAll(initialSections);
            }
        } catch (Exception e) {
            log.warn("Could not seed initial sections: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> getAll() {
        return sectionRepository.findAll().stream()
                .map(SectionResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public SectionResponse getById(Long id) {
        return SectionResponse.fromEntity(findByIdOrThrow(id));
    }

    public SectionResponse create(SectionRequest request) {
        String name = requireNonBlank(request.getName(), "Name is required").trim();
        if (sectionRepository.existsByName(name)) {
            throw new DuplicateResourceException(
                    "Section with name '" + name + "' already exists");
        }
        String status = (request.getStatus() == null || request.getStatus().isBlank()) ? "Active" : request.getStatus().trim();
        SectionEntity entity = SectionEntity.builder()
                .name(name)
                .status(status)
                .build();
        return SectionResponse.fromEntity(sectionRepository.save(entity));
    }

    public SectionResponse update(Long id, SectionRequest request) {
        SectionEntity entity = findByIdOrThrow(id);
        String name = requireNonBlank(request.getName(), "Name is required").trim();
        if (!entity.getName().equalsIgnoreCase(name) && sectionRepository.existsByName(name)) {
            throw new DuplicateResourceException(
                    "Section with name '" + name + "' already exists");
        }
        entity.setName(name);
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            entity.setStatus(request.getStatus().trim());
        }
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

package com.school.management.service;

import com.school.management.dto.SubjectRequest;
import com.school.management.dto.SubjectResponse;
import com.school.management.entity.SubjectEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.SubjectRepository;
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
public class SubjectService {

    private final SubjectRepository subjectRepository;

    @PostConstruct
    public void seedInitialSubjectsIfEmpty() {
        try {
            if (subjectRepository.count() == 0) {
                log.info("Seeding initial subjects...");
                List<SubjectEntity> initialSubjects = List.of(
                        SubjectEntity.builder().name("Mathematics").code("MATH-101").status("Active").build(),
                        SubjectEntity.builder().name("Science").code("SCI-101").status("Active").build(),
                        SubjectEntity.builder().name("English Literature").code("ENG-101").status("Active").build(),
                        SubjectEntity.builder().name("Social Studies").code("SST-101").status("Active").build(),
                        SubjectEntity.builder().name("Hindi").code("HIN-101").status("Active").build(),
                        SubjectEntity.builder().name("Computer Science").code("CS-101").status("Active").build(),
                        SubjectEntity.builder().name("Physics").code("PHY-101").status("Active").build(),
                        SubjectEntity.builder().name("Chemistry").code("CHE-101").status("Active").build(),
                        SubjectEntity.builder().name("Biology").code("BIO-101").status("Active").build(),
                        SubjectEntity.builder().name("Physical Education").code("PED-101").status("Active").build()
                );
                subjectRepository.saveAll(initialSubjects);
            }
        } catch (Exception e) {
            log.warn("Could not seed initial subjects: {}", e.getMessage());
        }
    }

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

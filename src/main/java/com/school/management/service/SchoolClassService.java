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
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final SectionRepository sectionRepository;
    private final JdbcTemplate jdbcTemplate;

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

        // 1. Remove teacher-class associations from join table
        try {
            jdbcTemplate.update("DELETE FROM teacher_classes WHERE class_id = ?", id);
        } catch (Exception e) {
            log.warn("Notice deleting from teacher_classes for class id {}: {}", id, e.getMessage());
        }

        // 2. Clean up comma-separated assigned_class_ids in teachers table
        try {
            cleanIdFromCommaSeparatedColumn("teachers", "assigned_class_ids", id);
        } catch (Exception e) {
            log.warn("Notice cleaning teachers.assigned_class_ids for class id {}: {}", id, e.getMessage());
        }

        schoolClassRepository.delete(entity);
        schoolClassRepository.flush();
    }

    private void cleanIdFromCommaSeparatedColumn(String table, String column, Long id) {
        String target = String.valueOf(id);
        String sql = "SELECT id, " + column + " FROM " + table + " WHERE " + column + " IS NOT NULL AND " + column + " != ''";
        jdbcTemplate.query(sql, rs -> {
            long rowId = rs.getLong("id");
            String raw = rs.getString(column);
            if (raw != null && !raw.isBlank()) {
                String[] parts = raw.split(",");
                List<String> remaining = new ArrayList<>();
                boolean found = false;
                for (String p : parts) {
                    String clean = p.trim();
                    if (!clean.isEmpty()) {
                        if (clean.equals(target)) {
                            found = true;
                        } else {
                            remaining.add(clean);
                        }
                    }
                }
                if (found) {
                    String updated = String.join(",", remaining);
                    jdbcTemplate.update("UPDATE " + table + " SET " + column + " = ? WHERE id = ?", updated, rowId);
                }
            }
        });
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

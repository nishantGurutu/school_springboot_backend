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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final JdbcTemplate jdbcTemplate;

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
                .className(request.getClassName() == null ? null : request.getClassName().trim())
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
        if (request.getClassName() != null) {
            entity.setClassName(request.getClassName().trim());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            entity.setStatus(request.getStatus().trim());
        }
        return SubjectResponse.fromEntity(subjectRepository.save(entity));
    }

    public void delete(Long id) {
        SubjectEntity entity = findByIdOrThrow(id);

        // 1. Remove references from teacher_subjects join table
        try {
            jdbcTemplate.update("DELETE FROM teacher_subjects WHERE subject_id = ?", id);
        } catch (Exception e) {
            log.warn("Notice deleting from teacher_subjects for subject id {}: {}", id, e.getMessage());
        }

        // 2. Remove curriculum topic notes referencing this subject
        try {
            jdbcTemplate.update("DELETE FROM topic_notes WHERE subject_id = ?", id);
        } catch (Exception e) {
            log.warn("Notice deleting from topic_notes for subject id {}: {}", id, e.getMessage());
        }

        // 3. Remove curriculum topics referencing this subject
        try {
            jdbcTemplate.update("DELETE FROM subject_topics WHERE subject_id = ?", id);
        } catch (Exception e) {
            log.warn("Notice deleting from subject_topics for subject id {}: {}", id, e.getMessage());
        }

        // 4. Remove curriculum chapters referencing this subject
        try {
            jdbcTemplate.update("DELETE FROM subject_chapters WHERE subject_id = ?", id);
        } catch (Exception e) {
            log.warn("Notice deleting from subject_chapters for subject id {}: {}", id, e.getMessage());
        }

        // 5. Unlink subject_id in homeworks if any
        try {
            jdbcTemplate.update("UPDATE homeworks SET subject_id = NULL WHERE subject_id = ?", id);
        } catch (Exception ignored) {}

        // 6. Clean up comma-separated subject_ids in teachers table
        try {
            cleanIdFromCommaSeparatedColumn("teachers", "subject_ids", id);
        } catch (Exception e) {
            log.warn("Notice cleaning teachers.subject_ids for subject id {}: {}", id, e.getMessage());
        }

        // 7. Clean up comma-separated subject_ids in students table
        try {
            cleanIdFromCommaSeparatedColumn("students", "subject_ids", id);
        } catch (Exception e) {
            log.warn("Notice cleaning students.subject_ids for subject id {}: {}", id, e.getMessage());
        }

        // 8. Delete subject entity and flush transaction
        subjectRepository.delete(entity);
        subjectRepository.flush();
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

    private SubjectEntity findByIdOrThrow(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));
    }
}

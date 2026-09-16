package com.school.management.service;

import com.school.management.dto.ExamRequest;
import com.school.management.dto.ExamResponse;
import com.school.management.entity.ExamEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamService {

    private final ExamRepository examRepository;

    @Transactional(readOnly = true)
    public List<ExamResponse> getAll() {
        return examRepository.findAll().stream()
                .map(ExamResponse::fromEntity)
                .toList();
    }

    public ExamResponse create(ExamRequest request) {
        String name = requireNonBlank(request.getName(), "Name is required");
        if (examRepository.existsByName(name.trim())) {
            throw new DuplicateResourceException(
                    "Exam with name '" + name + "' already exists");
        }
        ExamEntity entity = ExamEntity.builder()
                .name(name.trim())
                .date(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        return ExamResponse.fromEntity(examRepository.save(entity));
    }

    public ExamResponse update(Long id, ExamRequest request) {
        ExamEntity entity = findByIdOrThrow(id);
        String name = requireNonBlank(request.getName(), "Name is required");
        if (!entity.getName().equals(name.trim()) && examRepository.existsByName(name.trim())) {
            throw new DuplicateResourceException(
                    "Exam with name '" + name + "' already exists");
        }
        entity.setName(name.trim());
        entity.setDate(request.getDate());
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        return ExamResponse.fromEntity(examRepository.save(entity));
    }

    public void delete(Long id) {
        ExamEntity entity = findByIdOrThrow(id);
        examRepository.delete(entity);
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private ExamEntity findByIdOrThrow(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + id));
    }
}
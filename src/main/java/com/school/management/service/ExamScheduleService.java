package com.school.management.service;

import com.school.management.dto.ExamScheduleRequest;
import com.school.management.dto.ExamScheduleResponse;
import com.school.management.entity.ExamScheduleEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.ExamScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamScheduleService {

    private final ExamScheduleRepository examScheduleRepository;

    @Transactional(readOnly = true)
    public List<ExamScheduleResponse> getAll() {
        return examScheduleRepository.findAll().stream()
                .map(ExamScheduleResponse::fromEntity)
                .toList();
    }

    public ExamScheduleResponse create(ExamScheduleRequest request) {
        String className = requireNonBlank(request.getClassName(), "Class name is required");
        String subject = requireNonBlank(request.getSubject(), "Subject is required");
        if (examScheduleRepository.existsByClassNameAndSubject(className.trim(), subject.trim())) {
            throw new DuplicateResourceException(
                    "Schedule for class '" + className + "' and subject '" + subject + "' already exists");
        }
        ExamScheduleEntity entity = ExamScheduleEntity.builder()
                .className(className.trim())
                .subject(subject.trim())
                .date(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .duration(request.getDuration())
                .room(request.getRoom())
                .build();
        return ExamScheduleResponse.fromEntity(examScheduleRepository.save(entity));
    }

    public ExamScheduleResponse update(Long id, ExamScheduleRequest request) {
        ExamScheduleEntity entity = findByIdOrThrow(id);
        String className = requireNonBlank(request.getClassName(), "Class name is required");
        String subject = requireNonBlank(request.getSubject(), "Subject is required");
        boolean changed = !entity.getClassName().equals(className.trim()) || !entity.getSubject().equals(subject.trim());
        if (changed && examScheduleRepository.existsByClassNameAndSubject(className.trim(), subject.trim())) {
            throw new DuplicateResourceException(
                    "Schedule for class '" + className + "' and subject '" + subject + "' already exists");
        }
        entity.setClassName(className.trim());
        entity.setSubject(subject.trim());
        entity.setDate(request.getDate());
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());
        entity.setDuration(request.getDuration());
        entity.setRoom(request.getRoom());
        return ExamScheduleResponse.fromEntity(examScheduleRepository.save(entity));
    }

    public void delete(Long id) {
        ExamScheduleEntity entity = findByIdOrThrow(id);
        examScheduleRepository.delete(entity);
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private ExamScheduleEntity findByIdOrThrow(Long id) {
        return examScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam schedule not found with id: " + id));
    }
}
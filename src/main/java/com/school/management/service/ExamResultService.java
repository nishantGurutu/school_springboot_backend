package com.school.management.service;

import com.school.management.dto.ExamResultRequest;
import com.school.management.dto.ExamResultResponse;
import com.school.management.entity.ExamResultEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.ExamResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamResultService {

    private final ExamResultRepository examResultRepository;

    @Transactional(readOnly = true)
    public List<ExamResultResponse> getAll() {
        return examResultRepository.findAll().stream()
                .map(ExamResultResponse::fromEntity)
                .toList();
    }

    public ExamResultResponse create(ExamResultRequest request) {
        String admissionNo = requireNonBlank(request.getAdmissionNo(), "Admission number is required");
        String name = requireNonBlank(request.getName(), "Name is required");
        if (examResultRepository.existsByAdmissionNo(admissionNo.trim())) {
            throw new DuplicateResourceException(
                    "Exam result with admission number '" + admissionNo + "' already exists");
        }
        ExamResultEntity entity = ExamResultEntity.builder()
                .admissionNo(admissionNo.trim())
                .name(name.trim())
                .rollNo(request.getRollNo())
                .className(request.getClassName())
                .exam(request.getExam())
                .total(request.getTotal())
                .percent(request.getPercent())
                .grade(request.getGrade())
                .result(request.getResult())
                .avatar(request.getAvatar())
                .build();
        return ExamResultResponse.fromEntity(examResultRepository.save(entity));
    }

    public ExamResultResponse update(Long id, ExamResultRequest request) {
        ExamResultEntity entity = findByIdOrThrow(id);
        String admissionNo = requireNonBlank(request.getAdmissionNo(), "Admission number is required");
        String name = requireNonBlank(request.getName(), "Name is required");
        if (!entity.getAdmissionNo().equals(admissionNo.trim()) && examResultRepository.existsByAdmissionNo(admissionNo.trim())) {
            throw new DuplicateResourceException(
                    "Exam result with admission number '" + admissionNo + "' already exists");
        }
        entity.setAdmissionNo(admissionNo.trim());
        entity.setName(name.trim());
        entity.setRollNo(request.getRollNo());
        entity.setClassName(request.getClassName());
        entity.setExam(request.getExam());
        entity.setTotal(request.getTotal());
        entity.setPercent(request.getPercent());
        entity.setGrade(request.getGrade());
        entity.setResult(request.getResult());
        entity.setAvatar(request.getAvatar());
        return ExamResultResponse.fromEntity(examResultRepository.save(entity));
    }

    public void delete(Long id) {
        ExamResultEntity entity = findByIdOrThrow(id);
        examResultRepository.delete(entity);
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private ExamResultEntity findByIdOrThrow(Long id) {
        return examResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam result not found with id: " + id));
    }
}
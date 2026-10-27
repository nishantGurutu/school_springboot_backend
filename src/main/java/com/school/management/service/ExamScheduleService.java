package com.school.management.service;

import com.school.management.dto.ExamScheduleRequest;
import com.school.management.dto.ExamScheduleResponse;
import com.school.management.entity.ExamScheduleEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.ExamScheduleRepository;
import com.school.management.repository.SchoolClassRepository;
import com.school.management.repository.SectionRepository;
import com.school.management.repository.SubjectRepository;
import com.school.management.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamScheduleService {

    private final ExamScheduleRepository examScheduleRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final ExamRepository examRepository;

    @Transactional(readOnly = true)
    public List<ExamScheduleResponse> getAll() {
        return examScheduleRepository.findAll().stream()
                .map(ExamScheduleResponse::fromEntity)
                .toList();
    }

    public ExamScheduleResponse create(ExamScheduleRequest request) {
        validateSelection(request);
        String examName = request.getExamName().trim();
        String className = request.getClassName().trim();
        String section = request.getSection().trim();
        String subject = requireNonBlank(request.getSubject(), "Subject is required").trim();

        ExamScheduleEntity entity = ExamScheduleEntity.builder()
                .examName(examName)
                .className(className)
                .section(section)
                .subject(subject)
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
        validateSelection(request);
        if (request.getExamName() != null) {
            entity.setExamName(request.getExamName().trim());
        }
        if (request.getClassName() != null && !request.getClassName().isBlank()) {
            entity.setClassName(request.getClassName().trim());
        }
        if (request.getSection() != null) {
            entity.setSection(request.getSection().trim());
        }
        if (request.getSubject() != null && !request.getSubject().isBlank()) {
            entity.setSubject(request.getSubject().trim());
        }
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

    private void validateSelection(ExamScheduleRequest request) {
        String className = requireNonBlank(request.getClassName(), "Class is required").trim();
        String section = requireNonBlank(request.getSection(), "Section is required").trim();
        String subject = requireNonBlank(request.getSubject(), "Subject is required").trim();
        String exam = requireNonBlank(request.getExamName(), "Exam is required").trim();
        boolean linkedSection = schoolClassRepository.findAll().stream().anyMatch(c -> {
            if (!className.equalsIgnoreCase(c.getName())) return false;
            String assignedSection = c.getSection();
            if ((assignedSection == null || assignedSection.isBlank()) && c.getSectionId() != null) {
                assignedSection = sectionRepository.findById(c.getSectionId()).map(s -> s.getName()).orElse(null);
            }
            return assignedSection != null && section.equalsIgnoreCase(assignedSection.trim());
        });
        if (!linkedSection) throw new BadRequestException("Section does not belong to the selected class");
        boolean linkedSubject = subjectRepository.findByClassNameIgnoreCase(className).stream()
                .anyMatch(s -> subject.equalsIgnoreCase(s.getName()));
        if (!linkedSubject) throw new BadRequestException("Subject does not belong to the selected class");
        if (!examRepository.existsByName(exam)) throw new BadRequestException("Select an existing school exam");
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

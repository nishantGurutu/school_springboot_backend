package com.school.management.service;

import com.school.management.dto.TimetableRequest;
import com.school.management.dto.TimetableResponse;
import com.school.management.entity.TimetableEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.TimetableRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TimetableService {

    private final TimetableRepository timetableRepository;

    @Transactional(readOnly = true)
    public List<TimetableResponse> getAll(String className, String section, String dayOfWeek, String teacherName) {
        String c = (className != null && !className.isBlank() && !"all".equalsIgnoreCase(className.trim())) ? className.trim() : null;
        String s = (section != null && !section.isBlank() && !"all".equalsIgnoreCase(section.trim())) ? section.trim() : null;
        String d = (dayOfWeek != null && !dayOfWeek.isBlank() && !"all".equalsIgnoreCase(dayOfWeek.trim())) ? normalizeDay(dayOfWeek.trim()) : null;
        String t = (teacherName != null && !teacherName.isBlank()) ? teacherName.trim() : null;

        Specification<TimetableEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (c != null) {
                predicates.add(cb.equal(cb.lower(root.get("className")), c.toLowerCase()));
            }
            if (s != null) {
                predicates.add(cb.equal(cb.lower(root.get("section")), s.toLowerCase()));
            }
            if (d != null) {
                predicates.add(cb.equal(cb.lower(root.get("dayOfWeek")), d.toLowerCase()));
            }
            if (t != null) {
                predicates.add(cb.like(cb.lower(root.get("teacherName")), "%" + t.toLowerCase() + "%"));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        List<TimetableEntity> list = timetableRepository.findAll(spec);
        return list.stream()
                .map(TimetableResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public TimetableResponse getById(Long id) {
        return TimetableResponse.fromEntity(findByIdOrThrow(id));
    }

    public TimetableResponse create(TimetableRequest request) {
        validateRequest(request);

        TimetableEntity entity = TimetableEntity.builder()
                .className(request.getClassName().trim())
                .section(request.getSection().trim())
                .dayOfWeek(normalizeDay(request.getDayOfWeek().trim()))
                .periodName(request.getPeriodName() != null ? request.getPeriodName().trim() : "Period")
                .subject(request.getSubject().trim())
                .teacherId(request.getTeacherId())
                .teacherName(request.getTeacherName() != null ? request.getTeacherName().trim() : "")
                .classroom(request.getClassroom() != null ? request.getClassroom().trim() : "")
                .startTime(request.getStartTime().trim())
                .endTime(request.getEndTime().trim())
                .status((request.getStatus() != null && !request.getStatus().isBlank()) ? request.getStatus().trim() : "Active")
                .build();

        TimetableEntity saved = timetableRepository.save(entity);
        log.info("Created timetable slot: ID={}, Class={}, Section={}, Day={}, Subject={}",
                saved.getId(), saved.getClassName(), saved.getSection(), saved.getDayOfWeek(), saved.getSubject());
        return TimetableResponse.fromEntity(saved);
    }

    public TimetableResponse update(Long id, TimetableRequest request) {
        validateRequest(request);
        TimetableEntity entity = findByIdOrThrow(id);

        entity.setClassName(request.getClassName().trim());
        entity.setSection(request.getSection().trim());
        entity.setDayOfWeek(normalizeDay(request.getDayOfWeek().trim()));
        if (request.getPeriodName() != null) entity.setPeriodName(request.getPeriodName().trim());
        entity.setSubject(request.getSubject().trim());
        entity.setTeacherId(request.getTeacherId());
        if (request.getTeacherName() != null) entity.setTeacherName(request.getTeacherName().trim());
        if (request.getClassroom() != null) entity.setClassroom(request.getClassroom().trim());
        entity.setStartTime(request.getStartTime().trim());
        entity.setEndTime(request.getEndTime().trim());
        if (request.getStatus() != null && !request.getStatus().isBlank()) entity.setStatus(request.getStatus().trim());

        TimetableEntity updated = timetableRepository.save(entity);
        log.info("Updated timetable slot: ID={}", updated.getId());
        return TimetableResponse.fromEntity(updated);
    }

    public void delete(Long id) {
        TimetableEntity entity = findByIdOrThrow(id);
        timetableRepository.delete(entity);
        log.info("Deleted timetable slot: ID={}", id);
    }

    @Transactional(readOnly = true)
    public List<TimetableResponse> getByClassAndSection(String className, String section) {
        if (className == null || className.isBlank()) return List.of();
        String c = className.trim();
        String s = (section != null) ? section.trim() : null;

        List<TimetableEntity> list;
        if (s != null && !s.isBlank()) {
            list = timetableRepository.findByClassNameIgnoreCaseAndSectionIgnoreCase(c, s);
        } else {
            list = timetableRepository.findByClassNameIgnoreCase(c);
        }

        return list.stream().map(TimetableResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<TimetableResponse> getByTeacher(String teacherName) {
        if (teacherName == null || teacherName.isBlank()) return List.of();
        return timetableRepository.findByTeacherNameContainingIgnoreCase(teacherName.trim())
                .stream().map(TimetableResponse::fromEntity).toList();
    }

    private TimetableEntity findByIdOrThrow(Long id) {
        return timetableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable slot not found with id: " + id));
    }

    private void validateRequest(TimetableRequest request) {
        if (request.getClassName() == null || request.getClassName().isBlank()) {
            throw new BadRequestException("Class name is required");
        }
        if (request.getSection() == null || request.getSection().isBlank()) {
            throw new BadRequestException("Section is required");
        }
        if (request.getDayOfWeek() == null || request.getDayOfWeek().isBlank()) {
            throw new BadRequestException("Day of week is required");
        }
        if (request.getSubject() == null || request.getSubject().isBlank()) {
            throw new BadRequestException("Subject is required");
        }
        if (request.getStartTime() == null || request.getStartTime().isBlank()) {
            throw new BadRequestException("Start time is required");
        }
        if (request.getEndTime() == null || request.getEndTime().isBlank()) {
            throw new BadRequestException("End time is required");
        }
    }

    private String normalizeDay(String day) {
        if (day == null) return "Mon";
        String lower = day.trim().toLowerCase();
        if (lower.startsWith("mon")) return "Mon";
        if (lower.startsWith("tue")) return "Tue";
        if (lower.startsWith("wed")) return "Wed";
        if (lower.startsWith("thu")) return "Thu";
        if (lower.startsWith("fri")) return "Fri";
        if (lower.startsWith("sat")) return "Sat";
        if (lower.startsWith("sun")) return "Sun";
        return day;
    }
}

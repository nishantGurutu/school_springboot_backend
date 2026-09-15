package com.school.management.service.impl;

import com.school.management.domain.teacher.TeacherStatus;
import com.school.management.dto.TeacherRequest;
import com.school.management.dto.TeacherResponse;
import com.school.management.entity.TeacherEntity;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.TeacherRepository;
import com.school.management.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private static final int MAX_PAGE_SIZE = 200;

    private final TeacherRepository teacherRepository;

    @Override
    @Transactional
    public TeacherResponse create(TeacherRequest request) {
        if (teacherRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new DuplicateResourceException(
                    "Teacher with employee ID '" + request.getEmployeeId() + "' already exists");
        }
        if (teacherRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Teacher with email '" + request.getEmail() + "' already exists");
        }

        TeacherEntity teacher = TeacherEntity.builder()
                .employeeId(request.getEmployeeId().trim())
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .department(request.getDepartment().trim())
                .subject(request.getSubject().trim())
                .qualification(request.getQualification())
                .designation(request.getDesignation())
                .phone(request.getPhone().trim())
                .email(request.getEmail().trim())
                .address(request.getAddress())
                .joiningDate(request.getJoiningDate())
                .experienceYears(request.getExperienceYears())
                .bloodGroup(request.getBloodGroup())
                .jobType(request.getJobType() == null ? com.school.management.domain.teacher.JobType.FULL_TIME : request.getJobType())
                .avatar(request.getAvatar())
                .status(request.getStatus() == null ? TeacherStatus.ACTIVE : request.getStatus())
                .build();

        return TeacherResponse.fromEntity(teacherRepository.save(teacher));
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherResponse getById(Long id) {
        return TeacherResponse.fromEntity(findByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherResponse getByEmployeeId(String employeeId) {
        TeacherEntity teacher = teacherRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher not found with employee ID: " + employeeId));
        return TeacherResponse.fromEntity(teacher);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherResponse getByEmail(String email) {
        TeacherEntity teacher = teacherRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher not found with email: " + email));
        return TeacherResponse.fromEntity(teacher);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherResponse> getAll(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return teacherRepository.findAll(pageable).map(TeacherResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherResponse> search(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            return Page.empty();
        }
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return teacherRepository.search(query.trim(), pageable).map(TeacherResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherResponse> getByDepartment(String department, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return teacherRepository.findByDepartment(department, pageable).map(TeacherResponse::fromEntity);
    }

    @Override
    @Transactional
    public TeacherResponse update(Long id, TeacherRequest request) {
        TeacherEntity teacher = findByIdOrThrow(id);

        teacher.setFirstName(request.getFirstName().trim());
        teacher.setLastName(request.getLastName().trim());
        teacher.setDepartment(request.getDepartment().trim());
        teacher.setSubject(request.getSubject().trim());
        teacher.setQualification(request.getQualification());
        teacher.setDesignation(request.getDesignation());
        teacher.setPhone(request.getPhone().trim());
        teacher.setEmail(request.getEmail().trim());
        teacher.setAddress(request.getAddress());
        teacher.setJoiningDate(request.getJoiningDate());
        teacher.setExperienceYears(request.getExperienceYears());
        teacher.setBloodGroup(request.getBloodGroup());
        if (request.getJobType() != null) {
            teacher.setJobType(request.getJobType());
        }
        teacher.setAvatar(request.getAvatar());
        if (request.getStatus() != null) {
            teacher.setStatus(request.getStatus());
        }

        return TeacherResponse.fromEntity(teacherRepository.save(teacher));
    }

    @Override
    @Transactional
    public TeacherResponse updateStatus(Long id, String status) {
        TeacherEntity teacher = findByIdOrThrow(id);
        teacher.setStatus(parseStatus(status));
        return TeacherResponse.fromEntity(teacherRepository.save(teacher));
    }

    private TeacherStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new com.school.management.exceptions.BadRequestException("Status is required");
        }
        try {
            return TeacherStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new com.school.management.exceptions.BadRequestException(
                    "Invalid status '" + value + "'. Allowed values: " + java.util.Arrays.toString(TeacherStatus.values()));
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TeacherEntity teacher = findByIdOrThrow(id);
        teacherRepository.delete(teacher);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(TeacherStatus status) {
        return teacherRepository.countByStatus(status);
    }

    private TeacherEntity findByIdOrThrow(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String field = sortBy == null || sortBy.isBlank() ? "id" : sortBy;
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, field));
    }
}
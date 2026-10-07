package com.school.management.service.impl;

import com.school.management.domain.teacher.TeacherStatus;
import com.school.management.domain.user.Role;
import com.school.management.dto.TeacherRequest;
import com.school.management.dto.TeacherResponse;
import com.school.management.entity.TeacherEntity;
import com.school.management.entity.UserEntity;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.TeacherRepository;
import com.school.management.repository.UserRepository;
import com.school.management.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.school.management.entity.StaffEntity;
import com.school.management.entity.SchoolClassEntity;
import com.school.management.entity.SubjectEntity;
import com.school.management.repository.StaffRepository;
import com.school.management.repository.SchoolClassRepository;
import com.school.management.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private static final int MAX_PAGE_SIZE = 200;

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final PasswordEncoder passwordEncoder;

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
        if ("admin@school.com".equalsIgnoreCase(request.getEmail().trim())) {
            throw new DuplicateResourceException("Cannot register a teacher with the Super Admin email");
        }

        String rawType = request.getType();
        Role targetRole = Role.TEACHER;
        String normalizedType = "Teacher";
        if (rawType != null) {
            String cleanType = rawType.trim().toUpperCase();
            if (cleanType.equals("PRINCIPAL") || cleanType.contains("PRINCIP")) {
                targetRole = Role.PRINCIPAL;
                normalizedType = "Principal";
            } else if (cleanType.equals("STAFF") || cleanType.contains("STAFF")) {
                targetRole = Role.STAFF;
                normalizedType = "Staff";
            }
        }

        String finalDesignation = (request.getDesignation() != null && !request.getDesignation().isBlank())
                ? request.getDesignation().trim()
                : normalizedType;

        // Resolve assigned classes
        List<Long> targetClassIds = request.getAssignedClassIds() != null && !request.getAssignedClassIds().isEmpty()
                ? request.getAssignedClassIds()
                : request.getClassIds();

        Set<SchoolClassEntity> classes = new HashSet<>();
        String classSummary = request.getAssignedClass();
        String classIdsStr = "";

        if (targetClassIds != null && !targetClassIds.isEmpty()) {
            List<SchoolClassEntity> foundClasses = schoolClassRepository.findAllById(targetClassIds);
            classes.addAll(foundClasses);
            classIdsStr = targetClassIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            if (classSummary == null || classSummary.isBlank()) {
                classSummary = foundClasses.stream()
                        .map(c -> c.getSection() != null && !c.getSection().isBlank() ? c.getName() + " (" + c.getSection() + ")" : c.getName())
                        .collect(Collectors.joining(", "));
            }
        }

        // Resolve subject specializations
        List<Long> targetSubjectIds = request.getSubjectIds();
        Set<SubjectEntity> subjects = new HashSet<>();
        String subjectSummary = request.getSubject();
        String subjectIdsStr = "";

        if (targetSubjectIds != null && !targetSubjectIds.isEmpty()) {
            List<SubjectEntity> foundSubjects = subjectRepository.findAllById(targetSubjectIds);
            subjects.addAll(foundSubjects);
            subjectIdsStr = targetSubjectIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            if (subjectSummary == null || subjectSummary.isBlank() || "General".equalsIgnoreCase(subjectSummary)) {
                subjectSummary = foundSubjects.stream()
                        .map(SubjectEntity::getName)
                        .collect(Collectors.joining(", "));
            }
        }
        if (subjectSummary == null || subjectSummary.isBlank()) {
            subjectSummary = "General";
        }

        TeacherEntity teacher = TeacherEntity.builder()
                .employeeId(request.getEmployeeId().trim())
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .department(request.getDepartment() != null ? request.getDepartment().trim() : "Administration")
                .departmentId(request.getDepartmentId())
                .subject(subjectSummary)
                .subjectSpecializations(subjects)
                .subjectIds(subjectIdsStr)
                .assignedClasses(classes)
                .assignedClassIds(classIdsStr)
                .assignedClass(classSummary)
                .qualification(request.getQualification())
                .designation(finalDesignation)
                .designationId(request.getDesignationId())
                .type(normalizedType)
                .phone(request.getPhone().trim())
                .email(request.getEmail().trim())
                .address(request.getAddress())
                .joiningDate(request.getJoiningDate())
                .experienceYears(request.getExperienceYears())
                .bloodGroup(request.getBloodGroup())
                .jobType(request.getJobType() == null ? com.school.management.domain.teacher.JobType.FULL_TIME : request.getJobType())
                .avatar(request.getAvatar())
                .status(request.getStatus() == null ? TeacherStatus.ACTIVE : request.getStatus())
                .gender(request.getGender())
                .dob(request.getDob())
                .fatherName(request.getFatherName())
                .motherName(request.getMotherName())
                .maritalStatus(request.getMaritalStatus())
                .contractType(request.getContractType())
                .shift(request.getShift())
                .workLocation(request.getWorkLocation())
                .height(request.getHeight())
                .weight(request.getWeight())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankName(request.getBankName())
                .ifscCode(request.getIfscCode())
                .nationalIdNumber(request.getNationalIdNumber())
                .docName(request.getDocName())
                .prevSchoolName(request.getPrevSchoolName())
                .prevSchoolAddress(request.getPrevSchoolAddress())
                .permanentAddress(request.getPermanentAddress())
                .teacherBio(request.getTeacherBio())
                .build();

        TeacherEntity saved = teacherRepository.save(teacher);

        // Auto-create/sync User account for login based on selected type/role
        String rawPassword = (request.getPassword() != null && !request.getPassword().isBlank()) 
                ? request.getPassword().trim() : "password";
        String teacherEmail = saved.getEmail().trim();
        String teacherFullName = (saved.getFirstName() + " " + saved.getLastName()).trim();

        try {
            java.util.Optional<UserEntity> existingUser = userRepository.findByEmailIgnoreCase(teacherEmail);
            if (existingUser.isEmpty()) {
                userRepository.save(UserEntity.builder()
                        .email(teacherEmail)
                        .password(passwordEncoder.encode(rawPassword))
                        .name(teacherFullName)
                        .role(targetRole)
                        .enabled(true)
                        .build());
            } else {
                UserEntity user = existingUser.get();
                if (user.getRole() != Role.ADMIN && user.getRole() != Role.MASTER_ADMIN && user.getRole() != Role.SUPER_ADMIN) {
                    if (request.getPassword() != null && !request.getPassword().isBlank()) {
                        user.setPassword(passwordEncoder.encode(rawPassword));
                    }
                    user.setName(teacherFullName);
                    user.setRole(targetRole);
                    userRepository.save(user);
                }
            }
        } catch (Exception ignored) {
        }

        // If Principal or Staff, also sync StaffEntity so it appears in Staff / HRM modules
        if (targetRole == Role.PRINCIPAL || targetRole == Role.STAFF) {
            try {
                java.util.Optional<StaffEntity> existingStaff = staffRepository.findByEmail(teacherEmail);
                StaffEntity staff = existingStaff.orElseGet(StaffEntity::new);
                staff.setName(teacherFullName);
                staff.setEmail(teacherEmail);
                staff.setPhone(saved.getPhone());
                staff.setDesignation(saved.getDesignation());
                staff.setStaffType(normalizedType);
                staff.setRole(targetRole.name());
                staff.setStatus("Active");
                staff.setJoinDate(saved.getJoiningDate() != null ? saved.getJoiningDate().toString() : java.time.LocalDate.now().toString());
                staffRepository.save(staff);
            } catch (Exception ignored) {
            }
        }

        return TeacherResponse.fromEntity(saved);
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
        teacher.setDepartmentId(request.getDepartmentId());

        // Update assigned classes
        List<Long> targetClassIds = request.getAssignedClassIds() != null && !request.getAssignedClassIds().isEmpty()
                ? request.getAssignedClassIds()
                : request.getClassIds();
        if (targetClassIds != null) {
            List<SchoolClassEntity> foundClasses = schoolClassRepository.findAllById(targetClassIds);
            teacher.setAssignedClasses(new HashSet<>(foundClasses));
            teacher.setAssignedClassIds(targetClassIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
            String classSummary = request.getAssignedClass();
            if (classSummary == null || classSummary.isBlank()) {
                classSummary = foundClasses.stream()
                        .map(c -> c.getSection() != null && !c.getSection().isBlank() ? c.getName() + " (" + c.getSection() + ")" : c.getName())
                        .collect(Collectors.joining(", "));
            }
            teacher.setAssignedClass(classSummary);
        } else if (request.getAssignedClass() != null) {
            teacher.setAssignedClass(request.getAssignedClass());
        }

        // Update subject specializations
        List<Long> targetSubjectIds = request.getSubjectIds();
        if (targetSubjectIds != null) {
            List<SubjectEntity> foundSubjects = subjectRepository.findAllById(targetSubjectIds);
            teacher.setSubjectSpecializations(new HashSet<>(foundSubjects));
            teacher.setSubjectIds(targetSubjectIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
            String subjectSummary = request.getSubject();
            if (subjectSummary == null || subjectSummary.isBlank() || "General".equalsIgnoreCase(subjectSummary)) {
                subjectSummary = foundSubjects.stream()
                        .map(SubjectEntity::getName)
                        .collect(Collectors.joining(", "));
            }
            teacher.setSubject(subjectSummary != null && !subjectSummary.isBlank() ? subjectSummary : "General");
        } else if (request.getSubject() != null && !request.getSubject().isBlank()) {
            teacher.setSubject(request.getSubject().trim());
        }

        teacher.setQualification(request.getQualification());
        teacher.setDesignation(request.getDesignation());
        teacher.setDesignationId(request.getDesignationId());
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

        // Form fields
        if (request.getGender() != null) teacher.setGender(request.getGender());
        if (request.getDob() != null) teacher.setDob(request.getDob());
        if (request.getFatherName() != null) teacher.setFatherName(request.getFatherName());
        if (request.getMotherName() != null) teacher.setMotherName(request.getMotherName());
        if (request.getMaritalStatus() != null) teacher.setMaritalStatus(request.getMaritalStatus());
        if (request.getContractType() != null) teacher.setContractType(request.getContractType());
        if (request.getShift() != null) teacher.setShift(request.getShift());
        if (request.getWorkLocation() != null) teacher.setWorkLocation(request.getWorkLocation());
        if (request.getHeight() != null) teacher.setHeight(request.getHeight());
        if (request.getWeight() != null) teacher.setWeight(request.getWeight());
        if (request.getBankAccountNumber() != null) teacher.setBankAccountNumber(request.getBankAccountNumber());
        if (request.getBankName() != null) teacher.setBankName(request.getBankName());
        if (request.getIfscCode() != null) teacher.setIfscCode(request.getIfscCode());
        if (request.getNationalIdNumber() != null) teacher.setNationalIdNumber(request.getNationalIdNumber());
        if (request.getDocName() != null) teacher.setDocName(request.getDocName());
        if (request.getPrevSchoolName() != null) teacher.setPrevSchoolName(request.getPrevSchoolName());
        if (request.getPrevSchoolAddress() != null) teacher.setPrevSchoolAddress(request.getPrevSchoolAddress());
        if (request.getPermanentAddress() != null) teacher.setPermanentAddress(request.getPermanentAddress());
        if (request.getTeacherBio() != null) teacher.setTeacherBio(request.getTeacherBio());

        Role updatedRole = null;
        if (request.getType() != null && !request.getType().isBlank()) {
            String cleanType = request.getType().trim().toUpperCase();
            if (cleanType.equals("PRINCIPAL") || cleanType.contains("PRINCIP")) {
                teacher.setType("Principal");
                updatedRole = Role.PRINCIPAL;
            } else if (cleanType.equals("STAFF") || cleanType.contains("STAFF")) {
                teacher.setType("Staff");
                updatedRole = Role.STAFF;
            } else {
                teacher.setType("Teacher");
                updatedRole = Role.TEACHER;
            }
        }

        TeacherEntity updated = teacherRepository.save(teacher);

        // Sync UserEntity password, name, and role if changed
        final Role roleToApply = updatedRole;
        String updatedEmail = updated.getEmail().trim();
        userRepository.findByEmailIgnoreCase(updatedEmail).ifPresent(user -> {
            if (user.getRole() != Role.ADMIN && user.getRole() != Role.MASTER_ADMIN && user.getRole() != Role.SUPER_ADMIN) {
                if (request.getPassword() != null && !request.getPassword().isBlank()) {
                    user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
                }
                user.setName((updated.getFirstName() + " " + updated.getLastName()).trim());
                if (roleToApply != null) {
                    user.setRole(roleToApply);
                }
                userRepository.save(user);
            }
        });

        // Sync to StaffEntity if applicable
        if (roleToApply == Role.PRINCIPAL || roleToApply == Role.STAFF) {
            try {
                java.util.Optional<StaffEntity> existingStaff = staffRepository.findByEmail(updatedEmail);
                StaffEntity staff = existingStaff.orElseGet(StaffEntity::new);
                staff.setName((updated.getFirstName() + " " + updated.getLastName()).trim());
                staff.setEmail(updatedEmail);
                staff.setPhone(updated.getPhone());
                staff.setDesignation(updated.getDesignation());
                staff.setStaffType(updated.getType());
                staff.setRole(roleToApply.name());
                staff.setStatus("Active");
                staffRepository.save(staff);
            } catch (Exception ignored) {
            }
        }

        return TeacherResponse.fromEntity(updated);
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
        if (teacher.getEmail() != null) {
            userRepository.findByEmailIgnoreCase(teacher.getEmail()).ifPresent(userRepository::delete);
        }
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
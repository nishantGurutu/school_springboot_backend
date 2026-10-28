package com.school.management.service.impl;

import com.school.management.domain.teacher.TeacherStatus;
import com.school.management.domain.user.Role;
import com.school.management.dto.TeacherRequest;
import com.school.management.dto.TeacherResponse;
import com.school.management.entity.*;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.*;
import com.school.management.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final com.school.management.repository.transport.DriverProfileRepository driverRepository;
    private final com.school.management.repository.transport.BusAssignmentRepository busAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.school.management.service.DriverBusAllocationService busAllocation;


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

        Role targetRole = resolveTargetRole(
                request.getRole() != null ? request.getRole() : (request.getUserType() != null ? request.getUserType() : request.getType()),
                request.getDesignation(),
                request.getDepartment()
        );
        if (targetRole == Role.DRIVER && request.getBusId() == null) {
            throw new com.school.management.exceptions.BadRequestException("Select an available bus for this driver");
        }
        if (targetRole != Role.DRIVER && request.getBusId() != null) {
            throw new com.school.management.exceptions.BadRequestException("Bus assignment is available only for drivers");
        }
        String normalizedType = normalizeTypeFromRole(targetRole);

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
        String teacherFullName = (saved.getFirstName() + (saved.getLastName() != null && !saved.getLastName().isBlank() ? " " + saved.getLastName().trim() : "")).trim();

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

        // If Principal, Staff, Accountant, Librarian, or Driver, also sync StaffEntity so it appears in Staff / HRM modules
        if (targetRole != Role.TEACHER) {
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

        // If Driver, also sync DriverProfileEntity so it is immediately registered in Transport Module
        if (targetRole == Role.DRIVER && driverRepository != null) {
            try {
                java.util.Optional<com.school.management.entity.transport.DriverProfileEntity> existingDriver = driverRepository.findByEmailIgnoreCase(teacherEmail);
                if (existingDriver.isEmpty()) {
                    driverRepository.save(com.school.management.entity.transport.DriverProfileEntity.builder()
                            .schoolId("SCH-001")
                            .name(teacherFullName)
                            .email(teacherEmail)
                            .phone(saved.getPhone() != null ? saved.getPhone() : "+91 98000 00000")
                            .licenseNumber("DL-" + (saved.getEmployeeId() != null ? saved.getEmployeeId() : "IND" + (1000 + saved.getId())))
                            .licenseExpiryDate(java.time.LocalDate.now().plusYears(5))
                            .licenseCategory("COMMERCIAL_HEAVY")
                            .experienceYears(saved.getExperienceYears() != null ? saved.getExperienceYears() : 5)
                            .joiningDate(saved.getJoiningDate() != null ? saved.getJoiningDate() : java.time.LocalDate.now())
                            .verificationStatus("VERIFIED")
                            .status("ACTIVE")
                            .avatarUrl(saved.getAvatar())
                            .emergencyContact(saved.getPhone())
                            .address(saved.getAddress() != null ? saved.getAddress() : "Campus Staff Quarters")
                            .build());
                } else {
                    com.school.management.entity.transport.DriverProfileEntity d = existingDriver.get();
                    d.setSchoolId("SCH-001");
                    d.setName(teacherFullName);
                    d.setPhone(saved.getPhone() != null ? saved.getPhone() : d.getPhone());
                    d.setAvatarUrl(saved.getAvatar() != null ? saved.getAvatar() : d.getAvatarUrl());
                    d.setStatus("ACTIVE");
                    driverRepository.save(d);
                }
            } catch (Exception ignored) {
            }
        }

        if (targetRole == Role.DRIVER) assignDriverBus(teacherEmail, request.getBusId());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherResponse getById(Long id) {
        return toResponse(findByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherResponse getByEmployeeId(String employeeId) {
        TeacherEntity teacher = teacherRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher not found with employee ID: " + employeeId));
        return toResponse(teacher);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherResponse getByEmail(String email) {
        TeacherEntity teacher = teacherRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher not found with email: " + email));
        return toResponse(teacher);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherResponse> getAll(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return teacherRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherResponse> search(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            return Page.empty();
        }
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return teacherRepository.search(query.trim(), pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherResponse> getByDepartment(String department, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return teacherRepository.findByDepartment(department, pageable).map(this::toResponse);
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

        Role updatedRole = resolveTargetRole(
                request.getRole() != null ? request.getRole() : (request.getUserType() != null ? request.getUserType() : request.getType()),
                request.getDesignation() != null ? request.getDesignation() : teacher.getDesignation(),
                request.getDepartment() != null ? request.getDepartment() : teacher.getDepartment()
        );
        String updatedType = normalizeTypeFromRole(updatedRole);
        teacher.setType(updatedType);

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
        if (roleToApply != null && roleToApply != Role.TEACHER) {
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

        // Sync to DriverProfileEntity if role is DRIVER
        if (driverRepository != null) {
            try {
                if (roleToApply == Role.DRIVER) {
                    String teacherFullName = (updated.getFirstName() + " " + updated.getLastName()).trim();
                    java.util.Optional<com.school.management.entity.transport.DriverProfileEntity> existingDriver = driverRepository.findByEmailIgnoreCase(updatedEmail);
                    if (existingDriver.isPresent()) {
                        com.school.management.entity.transport.DriverProfileEntity d = existingDriver.get();
                        d.setSchoolId("SCH-001");
                        d.setName(teacherFullName);
                        d.setPhone(updated.getPhone() != null ? updated.getPhone() : d.getPhone());
                        d.setAvatarUrl(updated.getAvatar() != null ? updated.getAvatar() : d.getAvatarUrl());
                        d.setStatus("ACTIVE");
                        driverRepository.save(d);
                    } else {
                        driverRepository.save(com.school.management.entity.transport.DriverProfileEntity.builder()
                                .schoolId("SCH-001")
                                .name(teacherFullName)
                                .email(updatedEmail)
                                .phone(updated.getPhone() != null ? updated.getPhone() : "+91 98000 00000")
                                .licenseNumber("DL-" + (updated.getEmployeeId() != null ? updated.getEmployeeId() : "IND" + (1000 + updated.getId())))
                                .licenseExpiryDate(java.time.LocalDate.now().plusYears(5))
                                .licenseCategory("COMMERCIAL_HEAVY")
                                .experienceYears(updated.getExperienceYears() != null ? updated.getExperienceYears() : 5)
                                .joiningDate(updated.getJoiningDate() != null ? updated.getJoiningDate() : java.time.LocalDate.now())
                                .verificationStatus("VERIFIED")
                                .status("ACTIVE")
                                .avatarUrl(updated.getAvatar())
                                .emergencyContact(updated.getPhone())
                                .address(updated.getAddress() != null ? updated.getAddress() : "Campus Staff Quarters")
                                .build());
                    }
                } else {
                    driverRepository.findByEmailIgnoreCase(updatedEmail).ifPresent(driverRepository::delete);
                }
            } catch (Exception ignored) {
            }
        }

        if (roleToApply == Role.DRIVER && request.getBusId() != null) {
            assignDriverBus(updatedEmail, request.getBusId());
        }
        return toResponse(updated);
    }

    @Override
    @Transactional
    public TeacherResponse updateStatus(Long id, String status) {
        TeacherEntity teacher = findByIdOrThrow(id);
        teacher.setStatus(parseStatus(status));
        return toResponse(teacherRepository.save(teacher));
    }

    private TeacherResponse toResponse(TeacherEntity teacher) {
        TeacherResponse response = TeacherResponse.fromEntity(teacher);
        if ("Driver".equalsIgnoreCase(teacher.getType()) && teacher.getEmail() != null) {
            driverRepository.findByEmailIgnoreCase(teacher.getEmail().trim())
                    .ifPresent(driver -> response.setBusId(driver.getAssignedBusId()));
        }
        return response;
    }

    private void assignDriverBus(String email, Long busId) {
        var driver = driverRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new com.school.management.exceptions.BadRequestException("Driver profile could not be created"));
        busAllocation.assign(driver, busId);
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
        String email = teacher.getEmail() != null ? teacher.getEmail().trim().toLowerCase() : null;
        String phone = teacher.getPhone() != null ? teacher.getPhone().trim() : null;

        if (email != null && !email.isBlank()) {
            userRepository.findByEmailIgnoreCase(email).ifPresent(userRepository::delete);
            if (driverRepository != null) {
                driverRepository.findByEmailIgnoreCase(email).ifPresent(driver -> {
                    if (busAssignmentRepository != null) {
                        try {
                            busAssignmentRepository.deleteByDriverId(driver.getId());
                        } catch (Exception ignored) {}
                    }
                    try {
                        driverRepository.delete(driver);
                    } catch (Exception ignored) {}
                });
            }
            if (staffRepository != null) {
                try {
                    staffRepository.findByEmailIgnoreCase(email).ifPresent(staffRepository::delete);
                } catch (Exception ignored) {}
            }
        }
        if (phone != null && !phone.isBlank()) {
            if (staffRepository != null) {
                try {
                    staffRepository.findByPhone(phone).ifPresent(staffRepository::delete);
                } catch (Exception ignored) {}
            }
            if (driverRepository != null) {
                try {
                    driverRepository.findByPhone(phone).ifPresent(driver -> {
                        if (busAssignmentRepository != null) {
                            try {
                                busAssignmentRepository.deleteByDriverId(driver.getId());
                            } catch (Exception ignored) {}
                        }
                        driverRepository.delete(driver);
                    });
                } catch (Exception ignored) {}
            }
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

    public static Role resolveTargetRole(String requestedRoleOrType, String designation, String department) {
        String desig = designation != null ? designation.trim().toLowerCase() : "";
        String dept = department != null ? department.trim().toLowerCase() : "";
        String req = requestedRoleOrType != null ? requestedRoleOrType.trim().toUpperCase() : "";

        // 1. Check Designation keywords (Strongest indicator of specific school job)
        if (desig.contains("driver") || desig.contains("conductor")) {
            return Role.DRIVER;
        }
        if (desig.contains("princip") || desig.contains("headmaster") || desig.contains("headmistress") || desig.contains("director") || desig.contains("vice princip")) {
            return Role.PRINCIPAL;
        }
        if (desig.contains("account") || desig.contains("cashier") || desig.contains("finance") || desig.contains("bursar")) {
            return Role.ACCOUNTANT;
        }
        if (desig.contains("librar")) {
            return Role.LIBRARIAN;
        }
        if (desig.contains("teacher") || desig.contains("pgt") || desig.contains("tgt") || desig.contains("prt") ||
            desig.contains("faculty") || desig.contains("lecturer") || desig.contains("professor") || desig.contains("instructor") ||
            desig.contains("hod") || desig.contains("educator")) {
            return Role.TEACHER;
        }
        if (desig.contains("staff") || desig.contains("clerk") || desig.contains("assistant") || desig.contains("receptionist") ||
            desig.contains("peon") || desig.contains("guard") || desig.contains("security") || desig.contains("admin") ||
            desig.contains("caretaker") || desig.contains("warden") || desig.contains("lab assistant") || desig.contains("attendant")) {
            return Role.STAFF;
        }

        // 2. Check Department keywords
        if (dept.contains("transport") || dept.contains("driver")) {
            return Role.DRIVER;
        }
        if (dept.contains("leadership") || dept.contains("princip")) {
            return Role.PRINCIPAL;
        }
        if (dept.contains("account") || dept.contains("finance")) {
            return Role.ACCOUNTANT;
        }
        if (dept.contains("librar")) {
            return Role.LIBRARIAN;
        }
        if (dept.contains("teach") || dept.contains("acad")) {
            return Role.TEACHER;
        }
        if (dept.contains("admin") || dept.contains("office") || dept.contains("support") || dept.contains("staff") || dept.contains("maintenance")) {
            return Role.STAFF;
        }

        // 3. Explicit Role/Type when specific
        if (!req.isBlank()) {
            if (req.contains("DRIVER")) return Role.DRIVER;
            if (req.contains("PRINCIP")) return Role.PRINCIPAL;
            if (req.contains("ACCOUNT")) return Role.ACCOUNTANT;
            if (req.contains("LIBRAR")) return Role.LIBRARIAN;
            if (req.contains("TEACH")) return Role.TEACHER;
            if (req.contains("STAFF")) return Role.STAFF;
        }

        return Role.STAFF;
    }

    public static String normalizeTypeFromRole(Role role) {
        if (role == null) return "Staff";
        switch (role) {
            case PRINCIPAL: return "Principal";
            case ACCOUNTANT: return "Accountant";
            case LIBRARIAN: return "Librarian";
            case DRIVER: return "Driver";
            case TEACHER: return "Teacher";
            default: return "Staff";
        }
    }
}
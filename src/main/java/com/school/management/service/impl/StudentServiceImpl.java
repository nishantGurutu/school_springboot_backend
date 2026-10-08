package com.school.management.service.impl;

import com.school.management.domain.student.StudentStatus;
import com.school.management.dto.StudentRequest;
import com.school.management.dto.StudentResponse;
import com.school.management.entity.StudentEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.domain.user.Role;
import com.school.management.entity.UserEntity;
import com.school.management.domain.guardian.GuardianType;
import com.school.management.entity.GuardianEntity;
import com.school.management.repository.GuardianRepository;
import com.school.management.repository.StudentRepository;
import com.school.management.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.school.management.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private static final int MAX_PAGE_SIZE = 200;
    private static final DateTimeFormatter FORMATTER_DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATTER_DMY_DASH = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final GuardianRepository guardianRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public StudentResponse create(StudentRequest request) {
        if (studentRepository.existsByAdmissionNo(request.getAdmissionNo())) {
            throw new DuplicateResourceException(
                    "Student with admission number '" + request.getAdmissionNo() + "' already exists");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank() && studentRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Student with email '" + request.getEmail() + "' already exists");
        }

        StudentEntity student = mapToEntity(new StudentEntity(), request, true);
        StudentEntity saved = studentRepository.save(student);

        // Auto-create/sync User account for student login
        String userEmail = (request.getLoginEmail() != null && !request.getLoginEmail().isBlank())
                ? request.getLoginEmail().trim()
                : ((saved.getEmail() != null && !saved.getEmail().isBlank())
                    ? saved.getEmail().trim()
                    : (saved.getAdmissionNo().toLowerCase() + "@schooldesk.com"));

        String rawPassword = (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword().trim()
                : ((request.getLoginPassword() != null && !request.getLoginPassword().isBlank())
                    ? request.getLoginPassword().trim()
                    : "password");

        java.util.Optional<UserEntity> existingStudentUser = userRepository.findByEmailIgnoreCase(userEmail);
        if (existingStudentUser.isEmpty()) {
            userRepository.save(UserEntity.builder()
                    .email(userEmail)
                    .password(passwordEncoder.encode(rawPassword))
                    .name(saved.getName())
                    .role(Role.STUDENT)
                    .enabled(true)
                    .build());
        } else {
            UserEntity user = existingStudentUser.get();
            if (rawPassword != null && !rawPassword.isBlank()) {
                user.setPassword(passwordEncoder.encode(rawPassword));
            }
            user.setName(saved.getName());
            user.setRole(Role.STUDENT);
            userRepository.save(user);
        }

        // Auto-create/sync PARENT login account and Guardian directory record if guardian email is provided
        syncParentAccountAndGuardian(saved, (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword().trim()
                : ((request.getLoginPassword() != null && !request.getLoginPassword().isBlank()) ? request.getLoginPassword().trim() : "password"));

        return StudentResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getById(Long id) {
        return StudentResponse.fromEntity(findByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getByAdmissionNo(String admissionNo) {
        StudentEntity student = studentRepository.findByAdmissionNo(admissionNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found with admission number: " + admissionNo));
        return StudentResponse.fromEntity(student);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentResponse> getAll(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return studentRepository.findAll(pageable).map(StudentResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentResponse> search(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            return Page.empty();
        }
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return studentRepository.search(query.trim(), pageable).map(StudentResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentResponse> getByClass(String className, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        return studentRepository.findByClassName(className, pageable).map(StudentResponse::fromEntity);
    }

    @Override
    @Transactional
    public StudentResponse update(Long id, StudentRequest request) {
        StudentEntity student = findByIdOrThrow(id);
        StudentEntity updated = studentRepository.save(mapToEntity(student, request, false));

        // Sync student UserEntity
        String rawPassword = (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword().trim()
                : ((request.getLoginPassword() != null && !request.getLoginPassword().isBlank())
                    ? request.getLoginPassword().trim()
                    : null);
        String targetEmail = (request.getLoginEmail() != null && !request.getLoginEmail().isBlank())
                ? request.getLoginEmail().trim()
                : ((updated.getEmail() != null && !updated.getEmail().isBlank()) 
                    ? updated.getEmail().trim() 
                    : (updated.getAdmissionNo().toLowerCase() + "@schooldesk.com"));

        userRepository.findByEmailIgnoreCase(targetEmail).ifPresent(user -> {
            if (rawPassword != null) {
                user.setPassword(passwordEncoder.encode(rawPassword));
            }
            user.setName(updated.getName());
            user.setRole(Role.STUDENT);
            userRepository.save(user);
        });

        // Sync Parent account and Guardian record
        syncParentAccountAndGuardian(updated, rawPassword);

        return StudentResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public StudentResponse updateStatus(Long id, String status) {
        StudentEntity student = findByIdOrThrow(id);
        student.setStatus(parseStatus(status));
        return StudentResponse.fromEntity(studentRepository.save(student));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        StudentEntity student = findByIdOrThrow(id);
        String targetEmail = (student.getEmail() != null && !student.getEmail().isBlank()) 
                ? student.getEmail().trim() 
                : (student.getAdmissionNo().toLowerCase() + "@schooldesk.com");
        userRepository.findByEmailIgnoreCase(targetEmail).ifPresent(userRepository::delete);
        studentRepository.delete(student);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(StudentStatus status) {
        return studentRepository.countByStatus(status);
    }

    private StudentEntity mapToEntity(StudentEntity e, StudentRequest r, boolean create) {
        String fullName = requireNonBlank(r.getFullName(), "Full name is required");
        String[] parts = splitName(fullName);

        e.setName(fullName.trim());
        e.setFirstName(parts[0]);
        e.setLastName(parts[1]);

        if (create) {
            e.setAdmissionNo(requireNonBlank(r.getAdmissionNo(), "Admission number is required").trim());
        }
        e.setRollNo(requireNonBlank(r.getRollNumber(), "Roll number is required").trim());
        e.setClassName(requireNonBlank(r.getStudentClass(), "Class is required").trim());
        e.setSection(r.getSection() == null ? "" : r.getSection().trim());
        e.setCategory(r.getCategory());
        e.setAcademicYear(r.getAcademicYear());
        e.setGender(r.getGender());
        e.setDateOfBirth(parseDob(r.getDob()));
        e.setPhone(requireNonBlank(r.getPhone(), "Phone number is required").trim());
        e.setEmail(r.getEmail());
        e.setStudentPhoto(r.getStudentPhoto());

        e.setFatherName(r.getFatherName());
        e.setFatherPhone(r.getFatherPhone());
        e.setFatherOccupation(r.getFatherOccupation());
        e.setFatherPhoto(r.getFatherPhoto());
        e.setMotherName(r.getMotherName());
        e.setMotherPhone(r.getMotherPhone());
        e.setMotherOccupation(r.getMotherOccupation());
        e.setMotherPhoto(r.getMotherPhoto());

        e.setGuardianRelation(r.getGuardianRelation());
        e.setGuardianName(r.getGuardianName());
        e.setGuardianEmail(r.getGuardianEmail());
        e.setGuardianPhone(r.getGuardianPhone());
        e.setGuardianOccupation(r.getGuardianOccupation());
        e.setGuardianAddress(r.getGuardianAddress());
        e.setGuardianPhoto(r.getGuardianPhoto());

        e.setBloodGroup(r.getBloodGroup());
        e.setHeight(r.getHeight());
        e.setWeight(r.getWeight());

        e.setBankAccountNumber(r.getBankAccountNumber());
        e.setBankName(r.getBankName());
        e.setIfscCode(r.getIfscCode());
        e.setNationalIdNumber(r.getNationalIdNumber());

        e.setPrevSchoolName(r.getPrevSchoolName());
        e.setPrevSchoolAddress(r.getPrevSchoolAddress());

        e.setCurrentAddress(r.getCurrentAddress());
        e.setPermanentAddress(r.getPermanentAddress());

        e.setHostelName(r.getHostelName());
        e.setRoomNo(r.getRoomNo());

        e.setDocName(r.getDocName());
        e.setDocFile(r.getDocFile());
        e.setStudentNotes(r.getStudentNotes());

        e.setAttendancePercentage(r.getAttendancePercentage());
        if (r.getStatus() != null) {
            e.setStatus(r.getStatus());
        }
        if (r.getSubjectIds() != null) {
            String subjectIdsStr = r.getSubjectIds().stream()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(","));
            e.setSubjectIds(subjectIdsStr);
        }
        return e;
    }

    private StudentStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Status is required");
        }
        try {
            return StudentStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "Invalid status '" + value + "'. Allowed values: " + Arrays.toString(StudentStatus.values()));
        }
    }

    private LocalDate parseDob(String dob) {
        if (dob == null || dob.isBlank()) {
            return null;
        }
        String value = dob.trim();
        for (DateTimeFormatter formatter : List.of(FORMATTER_DMY, FORMATTER_DMY_DASH, DateTimeFormatter.ISO_LOCAL_DATE)) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        throw new BadRequestException(
                "Invalid date of birth '" + dob + "'. Expected format: dd/MM/yyyy (e.g. 15/04/2012)");
    }

    private String[] splitName(String fullName) {
        String trimmed = requireNonBlank(fullName, "Full name is required").trim();
        int idx = trimmed.lastIndexOf(' ');
        if (idx < 0) {
            return new String[]{trimmed, ""};
        }
        return new String[]{trimmed.substring(0, idx), trimmed.substring(idx + 1)};
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private StudentEntity findByIdOrThrow(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String field = sortBy == null || sortBy.isBlank() ? "id" : sortBy;
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, field));
    }

    private void syncParentAccountAndGuardian(StudentEntity student, String parentPassword) {
        if (student.getGuardianEmail() == null || student.getGuardianEmail().isBlank()) {
            return;
        }
        String gEmail = student.getGuardianEmail().trim();
        String gName = (student.getGuardianName() != null && !student.getGuardianName().isBlank())
                ? student.getGuardianName().trim()
                : "Parent of " + student.getName();
        String pass = (parentPassword != null && !parentPassword.isBlank()) ? parentPassword.trim() : "password";

        // 1. Sync UserEntity (PARENT role login)
        java.util.Optional<UserEntity> userOpt = userRepository.findByEmailIgnoreCase(gEmail);
        if (userOpt.isEmpty()) {
            userRepository.save(UserEntity.builder()
                    .email(gEmail)
                    .password(passwordEncoder.encode(pass))
                    .name(gName)
                    .role(Role.PARENT)
                    .enabled(true)
                    .build());
        } else {
            UserEntity u = userOpt.get();
            u.setRole(Role.PARENT);
            if (parentPassword != null && !parentPassword.isBlank()) {
                u.setPassword(passwordEncoder.encode(pass));
            }
            userRepository.save(u);
        }

        // 2. Sync GuardianEntity (Guardian Directory record)
        java.util.Optional<GuardianEntity> gOpt = guardianRepository.findByEmailIgnoreCase(gEmail);
        GuardianType gType = GuardianType.FATHER;
        if (student.getGuardianRelation() != null) {
            try {
                gType = GuardianType.valueOf(student.getGuardianRelation().trim().toUpperCase());
            } catch (Exception ignored) {
                gType = GuardianType.OTHER;
            }
        }

        if (gOpt.isEmpty()) {
            GuardianEntity newGuardian = GuardianEntity.builder()
                    .guardianType(gType)
                    .name(gName)
                    .phone(student.getGuardianPhone())
                    .email(gEmail)
                    .occupation(student.getGuardianOccupation())
                    .address(student.getGuardianAddress())
                    .feeStatus("Clear")
                    .studentAdmissionNo(student.getAdmissionNo())
                    .build();
            guardianRepository.save(newGuardian);
        } else {
            GuardianEntity existing = gOpt.get();
            if (existing.getName() == null || existing.getName().isBlank()) {
                existing.setName(gName);
            }
            if (student.getGuardianPhone() != null && !student.getGuardianPhone().isBlank()) {
                existing.setPhone(student.getGuardianPhone());
            }
            String currAdm = existing.getStudentAdmissionNo();
            if (currAdm == null || currAdm.isBlank()) {
                existing.setStudentAdmissionNo(student.getAdmissionNo());
            } else if (!currAdm.contains(student.getAdmissionNo())) {
                existing.setStudentAdmissionNo(currAdm + ", " + student.getAdmissionNo());
            }
            guardianRepository.save(existing);
        }
    }
}
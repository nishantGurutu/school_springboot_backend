package com.school.management.service.impl;

import com.school.management.domain.user.Role;
import com.school.management.dto.GuardianRequest;
import com.school.management.dto.GuardianResponse;
import com.school.management.entity.GuardianEntity;
import com.school.management.entity.StudentEntity;
import com.school.management.entity.UserEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.GuardianRepository;
import com.school.management.repository.StudentRepository;
import com.school.management.repository.UserRepository;
import com.school.management.service.GuardianService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuardianServiceImpl implements GuardianService {

    private static final int MAX_PAGE_SIZE = 200;

    private final GuardianRepository guardianRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public GuardianResponse create(GuardianRequest request) {
        String email = requireNonBlank(request.getEmail(), "Email is required").trim();
        String name = requireNonBlank(request.getGuardianName(), "Guardian name is required").trim();

        if (guardianRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Guardian with email '" + email + "' already exists");
        }
        validateStudentRef(request.getStudentAdmissionNo());

        GuardianEntity guardian = GuardianEntity.builder()
                .guardianType(request.getGuardianType())
                .name(name)
                .phone(request.getPhone())
                .occupation(request.getOccupation())
                .email(email)
                .address(request.getAddress())
                .photo(request.getPhoto())
                .feeStatus(request.getFeeStatus() == null ? "Clear" : request.getFeeStatus())
                .studentAdmissionNo(request.getStudentAdmissionNo())
                .build();

        GuardianEntity saved = guardianRepository.save(guardian);

        // Optionally create a PARENT login account
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            createParentLogin(email, name, request.getPassword());
        }

        return GuardianResponse.fromEntity(saved, resolveStudentName(saved.getStudentAdmissionNo()));
    }

    @Override
    @Transactional(readOnly = true)
    public GuardianResponse getById(Long id) {
        GuardianEntity guardian = findByIdOrThrow(id);
        return GuardianResponse.fromEntity(guardian, resolveStudentName(guardian.getStudentAdmissionNo()));
    }

    @Override
    @Transactional(readOnly = true)
    public GuardianResponse getByEmail(String email) {
        GuardianEntity guardian = guardianRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian not found with email: " + email));
        return GuardianResponse.fromEntity(guardian, resolveStudentName(guardian.getStudentAdmissionNo()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuardianResponse> getByStudent(String studentAdmissionNo) {
        return guardianRepository.findByStudentAdmissionNo(studentAdmissionNo).stream()
                .map(g -> GuardianResponse.fromEntity(g, resolveStudentName(g.getStudentAdmissionNo())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GuardianResponse> getAll(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<GuardianEntity> guardians = guardianRepository.findAll(pageable);
        Map<String, String> studentNames = batchResolveStudentNames(guardians.getContent());
        return guardians.map(g -> GuardianResponse.fromEntity(g, studentNames.get(g.getStudentAdmissionNo())));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GuardianResponse> search(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            return Page.empty();
        }
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        Page<GuardianEntity> guardians = guardianRepository.search(query.trim(), pageable);
        Map<String, String> studentNames = batchResolveStudentNames(guardians.getContent());
        return guardians.map(g -> GuardianResponse.fromEntity(g, studentNames.get(g.getStudentAdmissionNo())));
    }

    @Override
    @Transactional
    public GuardianResponse update(Long id, GuardianRequest request) {
        GuardianEntity guardian = findByIdOrThrow(id);
        String email = requireNonBlank(request.getEmail(), "Email is required").trim();

        guardianRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Guardian with email '" + email + "' already exists");
            }
        });
        validateStudentRef(request.getStudentAdmissionNo());

        guardian.setGuardianType(request.getGuardianType());
        guardian.setName(requireNonBlank(request.getGuardianName(), "Guardian name is required").trim());
        guardian.setPhone(request.getPhone());
        guardian.setOccupation(request.getOccupation());
        guardian.setEmail(email);
        guardian.setAddress(request.getAddress());
        guardian.setPhoto(request.getPhoto());
        if (request.getFeeStatus() != null) {
            guardian.setFeeStatus(request.getFeeStatus());
        }
        guardian.setStudentAdmissionNo(request.getStudentAdmissionNo());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            userRepository.findByEmail(email).ifPresent(user ->
                    user.setPassword(passwordEncoder.encode(request.getPassword())));
        }

        return GuardianResponse.fromEntity(guardianRepository.save(guardian), resolveStudentName(guardian.getStudentAdmissionNo()));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        GuardianEntity guardian = findByIdOrThrow(id);
        guardianRepository.delete(guardian);
    }

    private void validateStudentRef(String admissionNo) {
        if (admissionNo == null || admissionNo.isBlank()) {
            return;
        }
        if (studentRepository.findByAdmissionNo(admissionNo).isEmpty()) {
            throw new BadRequestException("No student found with admission number '" + admissionNo + "'");
        }
    }

    private void createParentLogin(String email, String name, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "Email '" + email + "' is already registered. Use a different email for the parent login.");
        }
        UserEntity user = UserEntity.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .name(name)
                .role(Role.PARENT)
                .build();
        userRepository.save(user);
    }

    private String resolveStudentName(String admissionNo) {
        if (admissionNo == null || admissionNo.isBlank()) {
            return null;
        }
        return studentRepository.findByAdmissionNo(admissionNo)
                .map(StudentEntity::getName)
                .orElse(null);
    }

    private Map<String, String> batchResolveStudentNames(List<GuardianEntity> guardians) {
        List<String> admissionNos = guardians.stream()
                .map(GuardianEntity::getStudentAdmissionNo)
                .filter(ad -> ad != null && !ad.isBlank())
                .distinct()
                .collect(Collectors.toList());
        if (admissionNos.isEmpty()) {
            return Map.of();
        }
        return studentRepository.findAllByAdmissionNoIn(admissionNos).stream()
                .collect(Collectors.toMap(StudentEntity::getAdmissionNo, StudentEntity::getName));
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private GuardianEntity findByIdOrThrow(Long id) {
        return guardianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian not found with id: " + id));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String field = sortBy == null || sortBy.isBlank() ? "id" : sortBy;
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, field));
    }
}
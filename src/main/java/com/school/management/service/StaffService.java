package com.school.management.service;

import com.school.management.domain.user.Role;
import com.school.management.dto.StaffRequest;
import com.school.management.dto.StaffResponse;
import com.school.management.entity.StaffEntity;
import com.school.management.entity.UserEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.StaffRepository;
import com.school.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<StaffResponse> getAll() {
        return staffRepository.findAll().stream().map(StaffResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public StaffResponse getById(Long id) {
        return StaffResponse.fromEntity(findByIdOrThrow(id));
    }

    @Transactional
    public StaffResponse create(StaffRequest request) {
        StaffEntity staff = mapToEntity(new StaffEntity(), request, true);
        StaffEntity saved = staffRepository.save(staff);

        // Auto-create/sync User account for mobile app login
        if (saved.getEmail() != null && !saved.getEmail().isBlank() && !userRepository.existsByEmail(saved.getEmail())) {
            userRepository.save(UserEntity.builder()
                    .email(saved.getEmail())
                    .password(passwordEncoder.encode("password"))
                    .name(saved.getName())
                    .role(Role.STAFF)
                    .enabled(true)
                    .build());
        }

        return StaffResponse.fromEntity(saved);
    }

    @Transactional
    public StaffResponse update(Long id, StaffRequest request) {
        StaffEntity staff = findByIdOrThrow(id);
        return StaffResponse.fromEntity(staffRepository.save(mapToEntity(staff, request, false)));
    }

    @Transactional
    public void delete(Long id) {
        StaffEntity staff = findByIdOrThrow(id);
        staffRepository.delete(staff);
    }

    private StaffEntity mapToEntity(StaffEntity e, StaffRequest r, boolean create) {
        e.setName(requireNonBlank(r.getName(), "Name is required").trim());
        e.setStaffType(r.getStaffType());
        e.setDesignation(r.getDesignation());
        e.setPhone(r.getPhone());
        e.setEmail(r.getEmail());
        e.setSalary(r.getSalary());
        e.setJoinDate(r.getJoinDate());
        e.setStatus(r.getStatus());
        return e;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private StaffEntity findByIdOrThrow(Long id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with id: " + id));
    }
}
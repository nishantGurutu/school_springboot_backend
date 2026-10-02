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

        // Auto-create/sync User account for staff login
        if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
            String staffEmail = saved.getEmail().trim();
            String rawPassword = (request.getPassword() != null && !request.getPassword().isBlank()) 
                    ? request.getPassword().trim() : "password";

            java.util.Optional<UserEntity> existing = userRepository.findByEmailIgnoreCase(staffEmail);
            if (existing.isEmpty()) {
                userRepository.save(UserEntity.builder()
                        .email(staffEmail)
                        .password(passwordEncoder.encode(rawPassword))
                        .name(saved.getName())
                        .role(Role.STAFF)
                        .enabled(true)
                        .build());
            } else {
                UserEntity user = existing.get();
                if (request.getPassword() != null && !request.getPassword().isBlank()) {
                    user.setPassword(passwordEncoder.encode(rawPassword));
                }
                user.setName(saved.getName());
                user.setRole(Role.STAFF);
                userRepository.save(user);
            }
        }

        return StaffResponse.fromEntity(saved);
    }

    @Transactional
    public StaffResponse update(Long id, StaffRequest request) {
        StaffEntity staff = findByIdOrThrow(id);
        StaffEntity updated = staffRepository.save(mapToEntity(staff, request, false));

        if (updated.getEmail() != null && !updated.getEmail().isBlank()) {
            userRepository.findByEmailIgnoreCase(updated.getEmail().trim()).ifPresent(user -> {
                if (request.getPassword() != null && !request.getPassword().isBlank()) {
                    user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
                }
                user.setName(updated.getName());
                user.setRole(Role.STAFF);
                userRepository.save(user);
            });
        }

        return StaffResponse.fromEntity(updated);
    }

    @Transactional
    public void delete(Long id) {
        StaffEntity staff = findByIdOrThrow(id);
        if (staff.getEmail() != null) {
            userRepository.findByEmailIgnoreCase(staff.getEmail().trim()).ifPresent(userRepository::delete);
        }
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
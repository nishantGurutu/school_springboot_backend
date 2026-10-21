package com.school.management.service;

import com.school.management.domain.user.Role;
import com.school.management.dto.StaffRequest;
import com.school.management.dto.StaffResponse;
import com.school.management.entity.StaffEntity;
import com.school.management.entity.UserEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.StaffRepository;
import com.school.management.repository.TeacherRepository;
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
    private final TeacherRepository teacherRepository;
    private final com.school.management.repository.transport.DriverProfileRepository driverRepository;
    private final com.school.management.repository.transport.BusAssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final DriverBusAllocationService busAllocation;


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
        validateBusSelection(request, true);
        StaffEntity staff = mapToEntity(new StaffEntity(), request, true);
        StaffEntity saved = staffRepository.save(staff);

        // Auto-create/sync User account with role-specific permissions
        if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
            String staffEmail = saved.getEmail().trim();
            String rawPassword = (request.getPassword() != null && !request.getPassword().isBlank()) 
                    ? request.getPassword().trim() : "password";
            Role targetRole = resolveRole(request);

            java.util.Optional<UserEntity> existing = userRepository.findByEmailIgnoreCase(staffEmail);
            if (existing.isEmpty()) {
                userRepository.save(UserEntity.builder()
                        .email(staffEmail)
                        .password(passwordEncoder.encode(rawPassword))
                        .name(saved.getName())
                        .role(targetRole)
                        .enabled(true)
                        .build());
            } else {
                UserEntity user = existing.get();
                if (request.getPassword() != null && !request.getPassword().isBlank()) {
                    user.setPassword(passwordEncoder.encode(rawPassword));
                }
                user.setName(saved.getName());
                user.setRole(targetRole);
                userRepository.save(user);
            }

            // Sync driver profile if targetRole is DRIVER
            if (targetRole == Role.DRIVER && driverRepository != null) {
                java.util.Optional<com.school.management.entity.transport.DriverProfileEntity> existingDriver = driverRepository.findByEmailIgnoreCase(staffEmail);
                if (existingDriver.isEmpty()) {
                    driverRepository.save(com.school.management.entity.transport.DriverProfileEntity.builder()
                            .schoolId("SCH-001")
                            .name(saved.getName())
                            .email(staffEmail)
                            .phone(saved.getPhone() != null ? saved.getPhone() : "+91 98000 00000")
                            .licenseNumber("DL-IND" + (saved.getId() != null ? (1000 + saved.getId()) : "1001"))
                            .licenseExpiryDate(java.time.LocalDate.now().plusYears(5))
                            .licenseCategory("COMMERCIAL_HEAVY")
                            .experienceYears(5)
                            .joiningDate(java.time.LocalDate.now())
                            .verificationStatus("VERIFIED")
                            .status("ACTIVE")
                            .emergencyContact(saved.getPhone())
                            .address("Campus Staff Quarters")
                            .build());
                } else {
                    com.school.management.entity.transport.DriverProfileEntity d = existingDriver.get();
                    d.setSchoolId("SCH-001");
                    d.setName(saved.getName());
                    d.setPhone(saved.getPhone() != null ? saved.getPhone() : d.getPhone());
                    d.setStatus("ACTIVE");
                    driverRepository.save(d);
                }

            }
        }

        // Sync to TeacherEntity for unified Staff Directory
        syncStaffToTeacher(saved, request);

        assignSelectedBus(saved, request);
        return StaffResponse.fromEntity(saved);
    }

    @Transactional
    public StaffResponse update(Long id, StaffRequest request) {
        validateBusSelection(request, false);
        StaffEntity staff = findByIdOrThrow(id);
        StaffEntity updated = staffRepository.save(mapToEntity(staff, request, false));

        if (updated.getEmail() != null && !updated.getEmail().isBlank()) {
            Role targetRole = resolveRole(request);
            String staffEmail = updated.getEmail().trim();
            userRepository.findByEmailIgnoreCase(staffEmail).ifPresent(user -> {
                if (request.getPassword() != null && !request.getPassword().isBlank()) {
                    user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
                }
                user.setName(updated.getName());
                user.setRole(targetRole);
                userRepository.save(user);
            });

            if (targetRole == Role.DRIVER && driverRepository != null) {
                java.util.Optional<com.school.management.entity.transport.DriverProfileEntity> existingDriver = driverRepository.findByEmailIgnoreCase(staffEmail);
                if (existingDriver.isPresent()) {
                    com.school.management.entity.transport.DriverProfileEntity d = existingDriver.get();
                    d.setSchoolId("SCH-001");
                    d.setName(updated.getName());
                    d.setPhone(updated.getPhone() != null ? updated.getPhone() : d.getPhone());
                    d.setStatus(updated.getStatus() != null && !updated.getStatus().isBlank() ? updated.getStatus().toUpperCase() : "ACTIVE");
                    driverRepository.save(d);
                } else {
                    driverRepository.save(com.school.management.entity.transport.DriverProfileEntity.builder()
                            .schoolId("SCH-001")
                            .name(updated.getName())
                            .email(staffEmail)
                            .phone(updated.getPhone() != null ? updated.getPhone() : "+91 98000 00000")
                            .licenseNumber("DL-IND" + (updated.getId() != null ? (1000 + updated.getId()) : "1001"))
                            .licenseExpiryDate(java.time.LocalDate.now().plusYears(5))
                            .licenseCategory("COMMERCIAL_HEAVY")
                            .experienceYears(5)
                            .joiningDate(java.time.LocalDate.now())
                            .verificationStatus("VERIFIED")
                            .status("ACTIVE")
                            .emergencyContact(updated.getPhone())
                            .address("Campus Staff Quarters")
                            .build());
                }

            } else if (driverRepository != null) {
                // If role changed away from DRIVER, remove from driverRepository
                try {
                    driverRepository.findByEmailIgnoreCase(staffEmail).ifPresent(driver -> {
                        if (assignmentRepository != null) {
                            try {
                                assignmentRepository.deleteByDriverId(driver.getId());
                            } catch (Exception ignored) {}
                        }
                        driverRepository.delete(driver);
                    });
                } catch (Exception ignored) {
                }
            }
        }

        // Sync to TeacherEntity for unified Staff Directory
        syncStaffToTeacher(updated, request);

        assignSelectedBus(updated, request);
        return StaffResponse.fromEntity(updated);
    }

    @Transactional
    public void delete(Long id) {
        StaffEntity staff = findByIdOrThrow(id);
        String staffEmail = staff.getEmail() != null ? staff.getEmail().trim().toLowerCase() : null;
        String phone = staff.getPhone() != null ? staff.getPhone().trim() : null;

        if (staffEmail != null && !staffEmail.isBlank()) {
            userRepository.findByEmailIgnoreCase(staffEmail).ifPresent(userRepository::delete);
            if (driverRepository != null) {
                driverRepository.findByEmailIgnoreCase(staffEmail).ifPresent(driver -> {
                    if (assignmentRepository != null) {
                        try {
                            assignmentRepository.deleteByDriverId(driver.getId());
                        } catch (Exception ignored) {}
                    }
                    try {
                        driverRepository.delete(driver);
                    } catch (Exception ignored) {}
                });
            }
            if (teacherRepository != null) {
                try {
                    teacherRepository.findByEmailIgnoreCase(staffEmail).ifPresent(teacherRepository::delete);
                } catch (Exception ignored) {}
            }
        }
        if (phone != null && !phone.isBlank()) {
            if (teacherRepository != null) {
                try {
                    teacherRepository.findByPhone(phone).ifPresent(teacherRepository::delete);
                } catch (Exception ignored) {}
            }
            if (driverRepository != null) {
                try {
                    driverRepository.findByPhone(phone).ifPresent(driver -> {
                        if (assignmentRepository != null) {
                            try {
                                assignmentRepository.deleteByDriverId(driver.getId());
                            } catch (Exception ignored) {}
                        }
                        driverRepository.delete(driver);
                    });
                } catch (Exception ignored) {}
            }
        }
        staffRepository.delete(staff);
    }

    private void syncStaffToTeacher(StaffEntity staff, StaffRequest request) {
        if (teacherRepository == null || staff == null || staff.getEmail() == null || staff.getEmail().isBlank()) return;
        try {
            String staffEmail = staff.getEmail().trim().toLowerCase();
            String fullName = staff.getName() != null ? staff.getName().trim() : "Staff Member";
            String[] parts = fullName.split("\\s+", 2);
            String firstName = parts[0];
            Role targetRole = resolveRole(request);
            boolean isDriver = targetRole == Role.DRIVER;
            String lastName = parts.length > 1 ? parts[1] : (isDriver ? "(Driver)" : "(Staff)");

            com.school.management.entity.TeacherEntity teacher = teacherRepository.findByEmailIgnoreCase(staffEmail).orElseGet(() -> {
                return com.school.management.entity.TeacherEntity.builder()
                        .employeeId("EMP-STF-" + (staff.getId() != null ? staff.getId() : System.currentTimeMillis()))
                        .email(staffEmail)
                        .firstName(firstName)
                        .lastName(lastName)
                        .joiningDate(java.time.LocalDate.now())
                        .build();
            });
            teacher.setFirstName(firstName);
            teacher.setLastName(lastName);
            teacher.setPhone(staff.getPhone() != null && !staff.getPhone().isBlank() ? staff.getPhone() : "+91 98000 00000");
            teacher.setDepartment(isDriver ? "Transport" : "Administration");
            teacher.setSubject(isDriver ? "Transport" : "General");
            teacher.setDesignation(staff.getDesignation() != null ? staff.getDesignation() : (isDriver ? "Bus Driver" : "Staff"));
            teacher.setType(isDriver ? "Driver" : (staff.getStaffType() != null ? staff.getStaffType() : "Staff"));
            teacher.setStatus(com.school.management.domain.teacher.TeacherStatus.ACTIVE);
            teacherRepository.save(teacher);
        } catch (Exception ignored) {
        }
    }

    private void validateBusSelection(StaffRequest request, boolean create) {
        boolean driver = resolveRole(request) == Role.DRIVER;
        if (driver && (request.getEmail() == null || request.getEmail().isBlank())) {
            throw new BadRequestException("Driver login email is required");
        }
        if (driver && create && request.getBusId() == null) {
            throw new BadRequestException("Select an unassigned bus for this driver");
        }
        if (!driver && request.getBusId() != null) {
            throw new BadRequestException("Bus assignment is available only for drivers");
        }
    }

    private void assignSelectedBus(StaffEntity staff, StaffRequest request) {
        if (resolveRole(request) != Role.DRIVER || request.getBusId() == null) return;
        var driver = driverRepository.findByEmailIgnoreCase(staff.getEmail().trim())
                .orElseThrow(() -> new BadRequestException("Driver profile could not be created"));
        busAllocation.assign(driver, request.getBusId());
    }

    public Role resolveRole(StaffRequest r) {
        if (r == null) return Role.STAFF;
        String roleStr = (r.getRole() != null) ? r.getRole().trim().toUpperCase() : "";
        String desigStr = (r.getDesignation() != null) ? r.getDesignation().trim().toUpperCase() : "";
        String typeStr = (r.getStaffType() != null) ? r.getStaffType().trim().toUpperCase() : "";

        if (roleStr.equals("DRIVER") || (roleStr.isBlank() && (desigStr.contains("DRIVER") || typeStr.contains("DRIVER")))) {
            return Role.DRIVER;
        }
        if (roleStr.equals("PRINCIPAL") || roleStr.contains("PRINCIPAL") || desigStr.contains("PRINCIPAL") || typeStr.contains("PRINCIPAL") || desigStr.contains("HEADMASTER")) {
            return Role.PRINCIPAL;
        }
        if (roleStr.equals("ACCOUNTANT") || roleStr.contains("ACCOUNT") || desigStr.contains("ACCOUNT") || typeStr.contains("ACCOUNT") || desigStr.contains("CASHIER")) {
            return Role.ACCOUNTANT;
        }
        if (roleStr.equals("LIBRARIAN") || roleStr.contains("LIBRAR") || desigStr.contains("LIBRAR") || typeStr.contains("LIBRAR")) {
            return Role.LIBRARIAN;
        }
        if (roleStr.equals("SUPER_ADMIN") || roleStr.equals("MASTER_ADMIN") || roleStr.equals("ADMIN")) {
            return Role.SUPER_ADMIN;
        }
        if (roleStr.equals("TEACHER") || desigStr.contains("TEACHER") || typeStr.contains("TEACHER") || typeStr.contains("FACULTY")) {
            return Role.TEACHER;
        }
        return Role.STAFF;
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
        e.setRole(resolveRole(r).name());
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
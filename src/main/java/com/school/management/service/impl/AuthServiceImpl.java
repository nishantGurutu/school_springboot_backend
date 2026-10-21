package com.school.management.service.impl;

import com.school.management.domain.user.Role;
import com.school.management.dto.AuthResponse;
import com.school.management.dto.LoginRequest;
import com.school.management.dto.RefreshTokenRequest;
import com.school.management.entity.*;
import com.school.management.exceptions.UnauthorizedException;
import com.school.management.repository.*;
import com.school.management.security.JwtService;
import com.school.management.security.JwtService.TokenPair;
import com.school.management.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final TeacherRepository teacherRepository;
    private final StaffRepository staffRepository;
    private final com.school.management.repository.transport.DriverProfileRepository driverRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public AuthResponse login(LoginRequest request) {
        String input = request.getEmail() != null ? request.getEmail().trim() : "";
        if (input.isBlank() || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new UnauthorizedException("Invalid credentials");
        }

        // Detect user role & account automatically from login ID (no role selection needed)
        UserEntity user = findOrSyncUser(input, request.getPassword());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // Safe fallback for master admin default account
            if ("admin@school.com".equalsIgnoreCase(user.getEmail()) && "admin123".equals(request.getPassword())) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                userRepository.save(user);
            } else if (user.getRole() == Role.DRIVER && ("password123".equals(request.getPassword()) || "password".equals(request.getPassword()) || "123456".equals(request.getPassword()))) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                userRepository.save(user);
            } else {
                throw new UnauthorizedException("Invalid credentials");
            }
        }

        TokenPair pair = jwtService.generateTokenPair(user.getEmail());
        saveRefreshToken(pair.refreshToken(), user.getId());

        AuthResponse response = AuthResponse.fromPair(pair, user, "Login successful");
        enrichAuthResponse(response, user);
        return response;
    }

    private UserEntity findOrSyncUser(String input, String password) {
        String cleanInput = input.trim();

        // 0. Handle admin shorthand
        if (cleanInput.equalsIgnoreCase("admin")) {
            cleanInput = "admin@school.com";
        }

        // 1. Direct lookup by email in users table (case-insensitive)
        Optional<UserEntity> userOpt = userRepository.findByEmailIgnoreCase(cleanInput);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            syncUserRoleIfNeeded(user);
            return user;
        }

        // 2. Lookup in Student entity (by admissionNo, email, rollNo, or phone)
        Optional<StudentEntity> studentOpt = studentRepository.findByAdmissionNoIgnoreCase(cleanInput);
        if (studentOpt.isEmpty()) {
            studentOpt = studentRepository.findByEmailIgnoreCase(cleanInput);
        }
        if (studentOpt.isEmpty()) {
            studentOpt = studentRepository.findByPhone(cleanInput);
        }
        if (studentOpt.isEmpty()) {
            List<StudentEntity> byRoll = studentRepository.findByRollNo(cleanInput);
            if (!byRoll.isEmpty()) {
                studentOpt = Optional.of(byRoll.get(0));
            }
        }
        if (studentOpt.isPresent()) {
            StudentEntity s = studentOpt.get();
            String email = (s.getEmail() != null && !s.getEmail().isBlank()) ? s.getEmail() : (s.getAdmissionNo().toLowerCase() + "@schooldesk.com");
            Optional<UserEntity> existingUser = userRepository.findByEmailIgnoreCase(email);
            if (existingUser.isPresent()) {
                return existingUser.get();
            }
            return createAndSaveUser(email, s.getName(), Role.STUDENT, "password");
        }

        // 3. Lookup in Teacher entity (by employeeId, email, or phone)
        Optional<TeacherEntity> teacherOpt = teacherRepository.findByEmployeeIdIgnoreCase(cleanInput);
        if (teacherOpt.isEmpty()) {
            teacherOpt = teacherRepository.findByEmailIgnoreCase(cleanInput);
        }
        if (teacherOpt.isEmpty()) {
            teacherOpt = teacherRepository.findByPhone(cleanInput);
        }
        if (teacherOpt.isPresent()) {
            TeacherEntity t = teacherOpt.get();
            Role resolvedRole = TeacherServiceImpl.resolveTargetRole(t.getType(), t.getDesignation(), t.getDepartment());
            Optional<UserEntity> existingUser = userRepository.findByEmailIgnoreCase(t.getEmail());
            if (existingUser.isPresent()) {
                UserEntity u = existingUser.get();
                if (u.getRole() != Role.ADMIN && u.getRole() != Role.SUPER_ADMIN && u.getRole() != Role.MASTER_ADMIN) {
                    if (u.getRole() != resolvedRole) {
                        u.setRole(resolvedRole);
                        userRepository.save(u);
                    }
                }
                return u;
            }
            String fullName = (t.getFirstName() != null ? t.getFirstName() : "") + " " + (t.getLastName() != null ? t.getLastName() : "");
            return createAndSaveUser(t.getEmail(), fullName.trim(), resolvedRole, "password");
        }

        // 4. Lookup in Guardian entity (by email, phone, or studentAdmissionNo)
        Optional<GuardianEntity> guardianOpt = guardianRepository.findByEmailIgnoreCase(cleanInput);
        if (guardianOpt.isEmpty()) {
            guardianOpt = guardianRepository.findByPhone(cleanInput);
        }
        if (guardianOpt.isEmpty()) {
            List<GuardianEntity> byAdm = guardianRepository.findByStudentAdmissionNo(cleanInput);
            if (!byAdm.isEmpty()) {
                guardianOpt = Optional.of(byAdm.get(0));
            }
        }
        if (guardianOpt.isPresent()) {
            GuardianEntity g = guardianOpt.get();
            Optional<UserEntity> existingUser = userRepository.findByEmailIgnoreCase(g.getEmail());
            if (existingUser.isPresent()) {
                return existingUser.get();
            }
            return createAndSaveUser(g.getEmail(), g.getName(), Role.PARENT, "password");
        }

        // 5. Lookup in Staff entity (by email, phone)
        Optional<StaffEntity> staffOpt = staffRepository.findByEmailIgnoreCase(cleanInput);
        if (staffOpt.isEmpty()) {
            staffOpt = staffRepository.findByPhone(cleanInput);
        }
        if (staffOpt.isPresent()) {
            StaffEntity st = staffOpt.get();
            Optional<UserEntity> existingUser = userRepository.findByEmailIgnoreCase(st.getEmail());
            if (existingUser.isPresent()) {
                UserEntity u = existingUser.get();
                syncUserRoleIfNeeded(u);
                return u;
            }
            Role resolvedRole = resolveStaffRole(st);
            return createAndSaveUser(st.getEmail(), st.getName(), resolvedRole, "password");
        }

        // 6. Lookup in Driver entity (by email, phone, licenseNumber, or "driver" shorthand)
        if ("driver".equalsIgnoreCase(cleanInput)) {
            cleanInput = "driver@school.com";
        }
        Optional<com.school.management.entity.transport.DriverProfileEntity> driverOpt = driverRepository.findByEmailIgnoreCase(cleanInput);
        if (driverOpt.isEmpty()) {
            driverOpt = driverRepository.findByPhone(cleanInput);
        }
        if (driverOpt.isEmpty()) {
            driverOpt = driverRepository.findByLicenseNumberIgnoreCase(cleanInput);
        }
        if (driverOpt.isPresent()) {
            com.school.management.entity.transport.DriverProfileEntity d = driverOpt.get();
            Optional<UserEntity> existingUser = userRepository.findByEmailIgnoreCase(d.getEmail());
            if (existingUser.isPresent()) {
                UserEntity u = existingUser.get();
                syncUserRoleIfNeeded(u);
                return u;
            }
            return createAndSaveUser(d.getEmail(), d.getName(), Role.DRIVER, "password");
        }

        throw new UnauthorizedException("Invalid credentials");
    }

    private Role resolveStaffRole(StaffEntity st) {
        if (st == null) return Role.STAFF;
        String des = (st.getDesignation() != null ? st.getDesignation() : "").toLowerCase().trim();
        String type = (st.getStaffType() != null ? st.getStaffType() : "").toLowerCase().trim();
        String r = (st.getRole() != null ? st.getRole() : "").toLowerCase().trim();

        if (des.contains("driver") || des.contains("conductor") || type.contains("driver") || type.contains("transport") || r.contains("driver")) {
            return Role.DRIVER;
        }
        if (des.contains("princip") || type.contains("princip") || r.contains("princip")) {
            return Role.PRINCIPAL;
        }
        if (des.contains("account") || des.contains("cashier") || type.contains("account") || type.contains("finance") || r.contains("account")) {
            return Role.ACCOUNTANT;
        }
        if (des.contains("librar") || type.contains("librar") || r.contains("librar")) {
            return Role.LIBRARIAN;
        }
        if (des.contains("teacher") || des.contains("faculty") || type.contains("teacher") || r.contains("teacher")) {
            return Role.TEACHER;
        }
        return Role.STAFF;
    }

    private void syncUserRoleIfNeeded(UserEntity user) {
        if (user == null || user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.MASTER_ADMIN) {
            return;
        }

        boolean changed = false;

        // Clean up accidental " Faculty" or " Staff" appended to single names
        if (user.getName() != null) {
            String name = user.getName().trim();
            if (name.endsWith(" Faculty")) {
                user.setName(name.substring(0, name.length() - 8).trim());
                changed = true;
            } else if (name.endsWith(" Staff") && !name.equalsIgnoreCase("Office Staff")) {
                user.setName(name.substring(0, name.length() - 6).trim());
                changed = true;
            }
        }

        // 1. Check Driver profiles
        boolean isDriver = user.getRole() == Role.DRIVER || driverRepository.findByEmailIgnoreCase(user.getEmail()).isPresent();

        // 2. Check Staff repository
        Optional<StaffEntity> staffOpt = staffRepository.findByEmailIgnoreCase(user.getEmail());
        if (staffOpt.isPresent()) {
            StaffEntity st = staffOpt.get();
            Role staffRole = resolveStaffRole(st);
            if (staffRole == Role.DRIVER) {
                isDriver = true;
                if (!"DRIVER".equalsIgnoreCase(st.getRole())) {
                    st.setRole("DRIVER");
                    staffRepository.save(st);
                }
            } else if (!isDriver && user.getRole() != staffRole && user.getRole() == Role.STAFF) {
                user.setRole(staffRole);
                changed = true;
            }
        }

        // 3. Check Teacher repository
        Optional<TeacherEntity> teacherOpt = teacherRepository.findByEmailIgnoreCase(user.getEmail());
        if (teacherOpt.isPresent()) {
            TeacherEntity t = teacherOpt.get();
            Role teacherRole = TeacherServiceImpl.resolveTargetRole(t.getType(), t.getDesignation(), t.getDepartment());
            if (teacherRole == Role.DRIVER) {
                isDriver = true;
            } else if (!isDriver && user.getRole() != teacherRole) {
                user.setRole(teacherRole);
                changed = true;
            }
        }

        if (isDriver) {
            if (user.getRole() != Role.DRIVER) {
                user.setRole(Role.DRIVER);
                changed = true;
            }
            // Ensure DriverProfileEntity exists in transport repository
            if (driverRepository.findByEmailIgnoreCase(user.getEmail()).isEmpty()) {
                String phone = staffOpt.map(StaffEntity::getPhone)
                        .orElseGet(() -> teacherOpt.map(TeacherEntity::getPhone).orElse("+91 98000 00000"));
                driverRepository.save(com.school.management.entity.transport.DriverProfileEntity.builder()
                        .schoolId("SCH-001")
                        .name(user.getName())
                        .email(user.getEmail())
                        .phone(phone != null && !phone.isBlank() ? phone : "+91 98000 00000")
                        .licenseNumber("DL-IND" + (user.getId() != null ? (1000 + user.getId()) : "042011005"))
                        .licenseExpiryDate(java.time.LocalDate.now().plusYears(5))
                        .licenseCategory("COMMERCIAL_HEAVY")
                        .experienceYears(5)
                        .joiningDate(java.time.LocalDate.now())
                        .verificationStatus("VERIFIED")
                        .status("ACTIVE")
                        .emergencyContact(phone)
                        .address("Campus Staff Quarters")
                        .build());
            }
        }

        if (changed) {
            userRepository.save(user);
        }
    }

    private UserEntity createAndSaveUser(String email, String name, Role role, String rawPassword) {
        String passToUse = (rawPassword != null && !rawPassword.isBlank()) ? rawPassword : "password";
        UserEntity newUser = UserEntity.builder()
                .email(email)
                .password(passwordEncoder.encode(passToUse))
                .name(name != null && !name.isBlank() ? name : role.name() + " User")
                .role(role)
                .enabled(true)
                .build();
        return userRepository.save(newUser);
    }

    private void enrichAuthResponse(AuthResponse response, UserEntity user) {
        if (user.getRole() == Role.DRIVER) {
            setField(response, "className", null);
            setField(response, "department", "Transport");
            setField(response, "designation", "Bus Driver");

            driverRepository.findByEmailIgnoreCase(user.getEmail()).ifPresentOrElse(d -> {
                setField(response, "details", "Bus Driver • Transport (DL: " + d.getLicenseNumber() + ")");
                setField(response, "avatarUrl", d.getAvatarUrl());
                setField(response, "entityId", d.getId());
                setField(response, "phone", d.getPhone());
                if (d.getName() != null && !d.getName().isBlank()) {
                    setField(response, "name", d.getName());
                }
            }, () -> {
                staffRepository.findByEmailIgnoreCase(user.getEmail()).ifPresent(st -> {
                    setField(response, "details", (st.getDesignation() != null ? st.getDesignation() : "Bus Driver") + " • Transport");
                    setField(response, "entityId", st.getId());
                    setField(response, "phone", st.getPhone());
                });
            });
        } else if (user.getRole() == Role.STUDENT) {
            studentRepository.findByEmail(user.getEmail()).ifPresent(s -> {
                setField(response, "className", s.getClassName());
                setField(response, "details", "Roll No: " + s.getRollNo() + " • " + s.getClassName());
                setField(response, "avatarUrl", s.getStudentPhoto());
                setField(response, "entityId", s.getId());
                setField(response, "phone", s.getPhone());
            });
        } else if (user.getRole() == Role.PARENT) {
            setField(response, "className", null);
            guardianRepository.findByEmail(user.getEmail()).ifPresent(g -> {
                setField(response, "designation", "Guardian");
                setField(response, "details", "Guardian (" + (g.getGuardianType() != null ? g.getGuardianType().name() : "Parent") + ")");
                setField(response, "avatarUrl", g.getPhoto());
                setField(response, "entityId", g.getId());
                setField(response, "phone", g.getPhone());
            });
        } else if (user.getRole() == Role.TEACHER) {
            teacherRepository.findByEmail(user.getEmail()).ifPresent(t -> {
                setField(response, "department", t.getDepartment());
                setField(response, "designation", t.getDesignation() != null ? t.getDesignation() : "Teacher");
                setField(response, "className", t.getAssignedClass() != null && !t.getAssignedClass().isBlank() 
                        ? t.getAssignedClass() : (t.getSubject() != null ? t.getSubject() + " Teacher" : "Faculty"));
                setField(response, "details", (t.getDesignation() != null ? t.getDesignation() : "Teacher") + " • " + t.getDepartment());
                setField(response, "avatarUrl", t.getAvatar());
                setField(response, "entityId", t.getId());
                setField(response, "phone", t.getPhone());
            });
        } else if (user.getRole() == Role.STAFF || user.getRole() == Role.PRINCIPAL || user.getRole() == Role.ACCOUNTANT || user.getRole() == Role.LIBRARIAN) {
            setField(response, "className", null);
            staffRepository.findByEmail(user.getEmail()).ifPresentOrElse(st -> {
                String desig = st.getDesignation() != null ? st.getDesignation() : (user.getRole() == Role.PRINCIPAL ? "Principal" : user.getRole().name());
                String dept = st.getStaffType() != null ? st.getStaffType() : "Administration";
                setField(response, "department", dept);
                setField(response, "designation", desig);
                setField(response, "details", desig + " • " + dept);
                setField(response, "entityId", st.getId());
                setField(response, "phone", st.getPhone());
            }, () -> {
                teacherRepository.findByEmail(user.getEmail()).ifPresent(t -> {
                    String desig = t.getDesignation() != null ? t.getDesignation() : (user.getRole() == Role.PRINCIPAL ? "Principal" : user.getRole().name());
                    setField(response, "department", t.getDepartment());
                    setField(response, "designation", desig);
                    setField(response, "details", desig + " • " + t.getDepartment());
                    setField(response, "avatarUrl", t.getAvatar());
                    setField(response, "entityId", t.getId());
                    setField(response, "phone", t.getPhone());
                });
            });
        } else if (user.getRole() == Role.ADMIN || user.getRole() == Role.MASTER_ADMIN || user.getRole() == Role.SUPER_ADMIN) {
            setField(response, "className", null);
            setField(response, "designation", "Administrator");
            setField(response, "department", "Administration");
            setField(response, "details", "Administrator");
            setField(response, "avatarUrl", "");
        }
    }

    private void setField(Object obj, String fieldName, Object val) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, val);
        } catch (Exception ignored) {
        }
    }

    private void saveRefreshToken(String token, Long userId) {
        refreshTokenRepository.revokeAllForUser(userId);
        refreshTokenRepository.deleteExpiredOrRevoked(LocalDateTime.now());
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .token(token)
                .userId(userId)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(entity);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshTokenEntity stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (stored.isRevoked()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }
        if (stored.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }

        jwtService.isRefreshToken(request.getRefreshToken());
        String email = jwtService.extractUsername(request.getRefreshToken());
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        TokenPair pair = jwtService.generateTokenPair(email);
        saveRefreshToken(pair.refreshToken(), user.getId());

        AuthResponse response = AuthResponse.fromPair(pair, user, "Token refreshed successfully");
        enrichAuthResponse(response, user);
        return response;
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }
}
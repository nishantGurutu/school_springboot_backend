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
            return userOpt.get();
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
            Optional<UserEntity> existingUser = userRepository.findByEmailIgnoreCase(t.getEmail());
            if (existingUser.isPresent()) {
                return existingUser.get();
            }
            String fullName = (t.getFirstName() != null ? t.getFirstName() : "") + " " + (t.getLastName() != null ? t.getLastName() : "");
            return createAndSaveUser(t.getEmail(), fullName.trim(), Role.TEACHER, "password");
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
                return existingUser.get();
            }
            return createAndSaveUser(st.getEmail(), st.getName(), Role.STAFF, "password");
        }

        throw new UnauthorizedException("Invalid credentials");
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
        if (user.getRole() == Role.STUDENT) {
            studentRepository.findByEmail(user.getEmail()).ifPresent(s -> {
                setField(response, "className", s.getClassName());
                setField(response, "details", "Roll No: " + s.getRollNo() + " • " + s.getClassName());
                setField(response, "avatarUrl", s.getStudentPhoto());
                setField(response, "entityId", s.getId());
                setField(response, "phone", s.getPhone());
            });
        } else if (user.getRole() == Role.PARENT) {
            guardianRepository.findByEmail(user.getEmail()).ifPresent(g -> {
                setField(response, "className", "Parent of " + (g.getStudentAdmissionNo() != null ? g.getStudentAdmissionNo() : "Student"));
                setField(response, "details", "Guardian (" + (g.getGuardianType() != null ? g.getGuardianType().name() : "Parent") + ")");
                setField(response, "avatarUrl", g.getPhoto());
                setField(response, "entityId", g.getId());
                setField(response, "phone", g.getPhone());
            });
        } else if (user.getRole() == Role.TEACHER) {
            teacherRepository.findByEmail(user.getEmail()).ifPresent(t -> {
                setField(response, "department", t.getDepartment());
                setField(response, "className", t.getSubject() != null ? t.getSubject() + " Teacher" : "Faculty");
                setField(response, "details", (t.getDesignation() != null ? t.getDesignation() : "Teacher") + " • " + t.getDepartment());
                setField(response, "avatarUrl", t.getAvatar());
                setField(response, "entityId", t.getId());
                setField(response, "phone", t.getPhone());
            });
        } else if (user.getRole() == Role.STAFF) {
            staffRepository.findByEmail(user.getEmail()).ifPresent(st -> {
                setField(response, "department", st.getStaffType());
                setField(response, "className", st.getDesignation() != null ? st.getDesignation() : "Staff Member");
                setField(response, "details", (st.getDesignation() != null ? st.getDesignation() : "Staff") + " (" + st.getStaffType() + ")");
                setField(response, "entityId", st.getId());
                setField(response, "phone", st.getPhone());
            });
        }
    }

    private void setField(Object obj, String fieldName, Object val) {
        if (val == null) return;
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
package com.school.management.controller;

import com.school.management.dto.UserProfileResponse;
import com.school.management.entity.UserEntity;
import com.school.management.exceptions.UnauthorizedException;
import com.school.management.repository.UserRepository;
import com.school.management.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Current user profile APIs")
public class ProfileController {

    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;

    @GetMapping("/me")
    @Operation(summary = "Get current logged-in user profile")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        UserEntity user = securityUtil.getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("No authenticated user"));
        return ResponseEntity.ok(UserProfileResponse.fromEntity(user));
    }
}
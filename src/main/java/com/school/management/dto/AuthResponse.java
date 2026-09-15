package com.school.management.dto;

import com.school.management.domain.user.Role;
import com.school.management.entity.UserEntity;
import com.school.management.security.JwtService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String message;
    private String accessToken;
    private String refreshToken;
    private long expiresAt;
    private String tokenType;
    private Long userId;
    private String email;
    private String name;
    private Role role;

    public static AuthResponse fromPair(JwtService.TokenPair pair, UserEntity user, String message) {
        return AuthResponse.builder()
                .message(message)
                .accessToken(pair.accessToken())
                .refreshToken(pair.refreshToken())
                .expiresAt(pair.expiresAt())
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .build();
    }
}
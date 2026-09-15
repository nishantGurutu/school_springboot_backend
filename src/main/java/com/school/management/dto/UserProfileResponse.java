package com.school.management.dto;

import com.school.management.domain.user.Role;
import com.school.management.entity.UserEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserProfileResponse {

    private Long id;
    private String email;
    private String name;
    private Role role;
    private boolean enabled;
    private LocalDateTime createdAt;

    public static UserProfileResponse fromEntity(UserEntity user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
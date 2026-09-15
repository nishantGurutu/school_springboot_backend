package com.school.management.util;

import com.school.management.entity.UserEntity;
import com.school.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtil {

    private final UserRepository userRepository;

    public Optional<UserEntity> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return Optional.empty();
        }

        String email = null;
        if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            email = userDetails.getUsername();
        } else if (auth.getPrincipal() instanceof String principalName) {
            email = principalName;
        }

        if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
            return Optional.empty();
        }

        return userRepository.findByEmail(email);
    }

    public Long getCurrentUserId() {
        return getCurrentUser().map(UserEntity::getId).orElse(null);
    }

    public String getCurrentUserEmail() {
        return getCurrentUser().map(UserEntity::getEmail).orElse(null);
    }
}

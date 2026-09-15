package com.school.management.service.impl;

import com.school.management.dto.AuthResponse;
import com.school.management.entity.RefreshTokenEntity;
import com.school.management.exceptions.UnauthorizedException;
import com.school.management.dto.LoginRequest;
import com.school.management.dto.RefreshTokenRequest;
import com.school.management.repository.RefreshTokenRepository;
import com.school.management.repository.UserRepository;
import com.school.management.security.JwtService;
import com.school.management.security.JwtService.TokenPair;
import com.school.management.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public AuthResponse login(LoginRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        TokenPair pair = jwtService.generateTokenPair(user.getEmail());
        saveRefreshToken(pair.refreshToken(), user.getId());

        return AuthResponse.fromPair(pair, user, "Login successful");
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

        return AuthResponse.fromPair(pair, user, "Token refreshed successfully");
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
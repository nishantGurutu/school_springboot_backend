package com.school.management.service;

import com.school.management.dto.AuthResponse;
import com.school.management.dto.LoginRequest;
import com.school.management.dto.RefreshTokenRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(RefreshTokenRequest request);
}

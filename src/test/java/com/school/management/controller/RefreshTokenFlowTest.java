package com.school.management.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.user.Role;
import com.school.management.entity.UserEntity;
import com.school.management.repository.UserRepository;
import com.school.management.repository.RefreshTokenRepository;
import com.school.management.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class RefreshTokenFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired RefreshTokenRepository tokens;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Autowired com.school.management.repository.PushDeviceRepository devices;
    final String email = "refresh.flow@test.local";
    void createUser() { users.save(UserEntity.builder().email(email).name("Refresh Flow").role(Role.ADMIN).password(encoder.encode("FlowSecret42!")).build()); }
    JsonNode login() throws Exception {
        return json.readTree(mvc.perform(post("/api/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("email", email, "password", "FlowSecret42!"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    JsonNode refresh(String token) throws Exception {
        return json.readTree(mvc.perform(post("/api/auth/refresh").contentType("application/json").content(json.writeValueAsString(Map.of("refreshToken", token))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    @Test void dashboardAndPhoneSessionsRefreshIndependentlyAndRotatedTokensCannotBeReused() throws Exception {
        createUser();
        var dashboard = login(); var phone = login();
        assertNotEquals(dashboard.get("refreshToken"), phone.get("refreshToken"));
        var refreshed = refresh(dashboard.get("refreshToken").asText());
        refresh(phone.get("refreshToken").asText());
        assertNotEquals(dashboard.get("accessToken"), refreshed.get("accessToken"));
        mvc.perform(get("/api/admin/runtime-config").header("Authorization", "Bearer " + refreshed.get("accessToken").asText())).andExpect(status().isOk());
        mvc.perform(post("/api/auth/refresh").contentType("application/json").content(json.writeValueAsString(Map.of("refreshToken", dashboard.get("refreshToken").asText())))).andExpect(status().isUnauthorized());
    }
    @Test void expiredAccessGets401ThenRefreshProvidesUsableAccessAndExpiredRefreshIsRejected() throws Exception {
        createUser(); var session = login();
        mvc.perform(get("/api/admin/runtime-config").header("Authorization", "Bearer " + io.jsonwebtoken.Jwts.builder().setSubject(email).claim("type", "access")
            .setExpiration(new java.util.Date(System.currentTimeMillis() - 60000))
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor("test-secret-key-for-unit-tests-only-long-enough".getBytes(java.nio.charset.StandardCharsets.UTF_8)), io.jsonwebtoken.SignatureAlgorithm.HS256).compact())).andExpect(status().isUnauthorized());
        var refreshed = refresh(session.get("refreshToken").asText());
        mvc.perform(get("/api/admin/runtime-config").header("Authorization", "Bearer " + refreshed.get("accessToken").asText())).andExpect(status().isOk());
        var row = tokens.findByToken(refreshed.get("refreshToken").asText()).orElseThrow();
        row.setExpiryDate(LocalDateTime.now().minusMinutes(1)); tokens.saveAndFlush(row);
        mvc.perform(post("/api/auth/refresh").contentType("application/json").content(json.writeValueAsString(Map.of("refreshToken", row.getToken())))).andExpect(status().isUnauthorized());
    }
    @Test void loginPersistsDeviceTokenForNotificationsAndDashboardLoginKeepsIt() throws Exception {
        createUser();
        String device = "fcm-device-token-for-login-flow-123456789";
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("email", email, "password", "FlowSecret42!", "deviceToken", device))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.deviceToken").doesNotExist());
        }
        var user = users.findByEmail(email).orElseThrow();
        assertEquals(1, devices.findByUserId(user.getId()).size());
        assertEquals(device, devices.findByUserId(user.getId()).get(0).getToken());
        login();
        assertEquals(1, devices.findByUserId(user.getId()).size());
        mvc.perform(post("/api/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("email", email, "password", "wrong", "deviceToken", "fcm-should-never-be-saved-123456789"))))
            .andExpect(status().isUnauthorized());
        assertEquals(1, devices.findByUserId(user.getId()).size());
    }
    @Test void sameDeviceReassignsToAuthenticatedUserRatherThanSendingPreviousUsersPush() throws Exception {
        createUser();
        users.save(UserEntity.builder().email("second.refresh@test.local").name("Second User").role(Role.ADMIN).password(encoder.encode("FlowSecret42!")).build());
        String device = "fcm-device-token-reassigned-123456789";
        for (String account : new String[]{email, "second.refresh@test.local"}) {
            mvc.perform(post("/api/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("email", account, "password", "FlowSecret42!", "deviceToken", device))))
                .andExpect(status().isOk());
        }
        assertTrue(devices.findByUserId(users.findByEmail(email).orElseThrow().getId()).isEmpty());
        assertEquals(device, devices.findByUserId(users.findByEmail("second.refresh@test.local").orElseThrow().getId()).get(0).getToken());
    }

}

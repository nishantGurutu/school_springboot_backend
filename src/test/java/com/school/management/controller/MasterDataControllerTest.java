package com.school.management.controller;

import com.school.management.domain.user.Role;
import com.school.management.dto.MasterDataResponse;
import com.school.management.entity.UserEntity;
import com.school.management.service.MasterDataService;
import com.school.management.util.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MasterDataControllerTest {

    @Mock
    private MasterDataService masterDataService;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private MasterDataController masterDataController;

    @Test
    void testGetMasterData_Success() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .email("admin@school.com")
                .name("Super Admin")
                .role(Role.ADMIN)
                .build();

        Map<String, Object> userDetails = new LinkedHashMap<>();
        userDetails.put("id", 1L);
        userDetails.put("name", "Super Admin");
        userDetails.put("email", "admin@school.com");
        userDetails.put("role", "ADMIN");

        MasterDataResponse mockResponse = MasterDataResponse.builder()
                .success(true)
                .message("Master data retrieved successfully")
                .userDetails(userDetails)
                .settings(new LinkedHashMap<>())
                .build();

        when(securityUtil.getCurrentUser()).thenReturn(Optional.of(user));
        when(masterDataService.getMasterData(user)).thenReturn(mockResponse);

        ResponseEntity<MasterDataResponse> response = masterDataController.getMasterData();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Super Admin", response.getBody().getUserDetails().get("name"));
        assertNotNull(response.getBody().getSettings());
    }
}

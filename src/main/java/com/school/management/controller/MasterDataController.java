package com.school.management.controller;

import com.school.management.dto.MasterDataResponse;
import com.school.management.entity.UserEntity;
import com.school.management.exceptions.UnauthorizedException;
import com.school.management.service.MasterDataService;
import com.school.management.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/master-data")
@RequiredArgsConstructor
@Tag(name = "Master Data", description = "Enterprise Master Data API providing complete userDetails and future settings")
public class MasterDataController {

    private final MasterDataService masterDataService;
    private final SecurityUtil securityUtil;

    @GetMapping
    @Operation(summary = "Get master data containing complete userDetails and future settings")
    public ResponseEntity<MasterDataResponse> getMasterData() {
        UserEntity currentUser = securityUtil.getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));
        return ResponseEntity.ok(masterDataService.getMasterData(currentUser));
    }
}

package com.school.management.controller;

import com.school.management.entity.BannerEntity;
import com.school.management.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
@Tag(name = "Banners", description = "Promotional Banner Management APIs for Mobile App & Web Dashboard")
public class BannerController {

    private final BannerService bannerService;

    @GetMapping
    @Operation(summary = "Get all promotional banners")
    public ResponseEntity<List<BannerEntity>> getAllBanners() {
        return ResponseEntity.ok(bannerService.getAll());
    }

    @GetMapping("/active")
    @Operation(summary = "Get only active promotional banners")
    public ResponseEntity<List<BannerEntity>> getActiveBanners() {
        return ResponseEntity.ok(bannerService.getActiveBanners());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get banner by ID")
    public ResponseEntity<BannerEntity> getBannerById(@PathVariable Long id) {
        return ResponseEntity.ok(bannerService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new promotional banner")
    public ResponseEntity<BannerEntity> createBanner(@RequestBody BannerEntity banner) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bannerService.create(banner));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update promotional banner")
    public ResponseEntity<BannerEntity> updateBanner(@PathVariable Long id, @RequestBody BannerEntity banner) {
        return ResponseEntity.ok(bannerService.update(id, banner));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete promotional banner")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        bannerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

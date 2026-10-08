package com.school.management.controller;

import com.school.management.entity.GalleryAlbumEntity;
import com.school.management.service.GalleryAlbumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gallery")
@RequiredArgsConstructor
@Tag(name = "Gallery", description = "School Photo Gallery & Albums Management APIs for Mobile App & Web Dashboard")
public class GalleryAlbumController {

    private final GalleryAlbumService galleryAlbumService;

    @GetMapping
    @Operation(summary = "Get all gallery albums")
    public ResponseEntity<List<GalleryAlbumEntity>> getAllAlbums() {
        return ResponseEntity.ok(galleryAlbumService.getAll());
    }

    @GetMapping("/active")
    @Operation(summary = "Get only active gallery albums")
    public ResponseEntity<List<GalleryAlbumEntity>> getActiveAlbums() {
        return ResponseEntity.ok(galleryAlbumService.getActiveAlbums());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get album by ID")
    public ResponseEntity<GalleryAlbumEntity> getAlbumById(@PathVariable Long id) {
        return ResponseEntity.ok(galleryAlbumService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new gallery album")
    public ResponseEntity<GalleryAlbumEntity> createAlbum(@RequestBody GalleryAlbumEntity album) {
        return ResponseEntity.status(HttpStatus.CREATED).body(galleryAlbumService.create(album));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update gallery album")
    public ResponseEntity<GalleryAlbumEntity> updateAlbum(@PathVariable Long id, @RequestBody GalleryAlbumEntity album) {
        return ResponseEntity.ok(galleryAlbumService.update(id, album));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete gallery album")
    public ResponseEntity<Void> deleteAlbum(@PathVariable Long id) {
        galleryAlbumService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

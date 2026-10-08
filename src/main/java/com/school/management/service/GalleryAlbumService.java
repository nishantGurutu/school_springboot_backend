package com.school.management.service;

import com.school.management.entity.GalleryAlbumEntity;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.GalleryAlbumRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GalleryAlbumService {

    private final GalleryAlbumRepository galleryAlbumRepository;

    @PostConstruct
    public void seedInitialAlbumsIfEmpty() {
        try {
            if (galleryAlbumRepository.count() == 0) {
                log.info("Seeding initial school gallery albums...");
                List<GalleryAlbumEntity> initialAlbums = List.of(
                        GalleryAlbumEntity.builder()
                                .title("Festival")
                                .description("School cultural & festival celebrations")
                                .photoCount(5)
                                .coverImage("https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80")
                                .status("Active")
                                .displayOrder(1)
                                .build(),
                        GalleryAlbumEntity.builder()
                                .title("Class Room Decoration")
                                .description("Creative art and classroom decoration activities")
                                .photoCount(4)
                                .coverImage("https://images.unsplash.com/photo-1580582932707-520aed937b7b?w=600&auto=format&fit=crop&q=80")
                                .status("Active")
                                .displayOrder(2)
                                .build(),
                        GalleryAlbumEntity.builder()
                                .title("Sports Day")
                                .description("Athletics championship and medals distribution")
                                .photoCount(6)
                                .coverImage("https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=600&auto=format&fit=crop&q=80")
                                .status("Active")
                                .displayOrder(3)
                                .build(),
                        GalleryAlbumEntity.builder()
                                .title("Annual Function")
                                .description("Annual day celebrations, drama, and performances")
                                .photoCount(8)
                                .coverImage("https://images.unsplash.com/photo-1523240795612-9a054b0db644?w=600&auto=format&fit=crop&q=80")
                                .status("Active")
                                .displayOrder(4)
                                .build()
                );
                galleryAlbumRepository.saveAll(initialAlbums);
            }
        } catch (Exception e) {
            log.warn("Could not seed initial gallery albums: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<GalleryAlbumEntity> getAll() {
        return galleryAlbumRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<GalleryAlbumEntity> getActiveAlbums() {
        return galleryAlbumRepository.findByStatusIgnoreCaseOrderByDisplayOrderAsc("Active");
    }

    @Transactional(readOnly = true)
    public GalleryAlbumEntity getById(Long id) {
        return galleryAlbumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery album not found with id: " + id));
    }

    public GalleryAlbumEntity create(GalleryAlbumEntity album) {
        if (album.getTitle() == null || album.getTitle().isBlank()) {
            album.setTitle("School Album");
        }
        if (album.getStatus() == null || album.getStatus().isBlank()) {
            album.setStatus("Active");
        }
        if (album.getPhotoCount() == null || album.getPhotoCount() < 1) {
            album.setPhotoCount(1);
        }
        return galleryAlbumRepository.save(album);
    }

    public GalleryAlbumEntity update(Long id, GalleryAlbumEntity album) {
        GalleryAlbumEntity existing = getById(id);
        if (album.getTitle() != null && !album.getTitle().isBlank()) existing.setTitle(album.getTitle());
        if (album.getDescription() != null) existing.setDescription(album.getDescription());
        if (album.getCoverImage() != null) existing.setCoverImage(album.getCoverImage());
        if (album.getPhotoCount() != null) existing.setPhotoCount(album.getPhotoCount());
        if (album.getStatus() != null && !album.getStatus().isBlank()) existing.setStatus(album.getStatus());
        if (album.getDisplayOrder() != null) existing.setDisplayOrder(album.getDisplayOrder());
        return galleryAlbumRepository.save(existing);
    }

    public void delete(Long id) {
        GalleryAlbumEntity existing = getById(id);
        galleryAlbumRepository.delete(existing);
    }
}

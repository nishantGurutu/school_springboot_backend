package com.school.management.service;

import com.school.management.entity.GalleryAlbumEntity;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.GalleryAlbumRepository;
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
        if (album.getTitle() != null && !album.getTitle().isBlank())
            existing.setTitle(album.getTitle());
        if (album.getDescription() != null)
            existing.setDescription(album.getDescription());
        if (album.getCoverImage() != null)
            existing.setCoverImage(album.getCoverImage());
        if (album.getPhotoCount() != null)
            existing.setPhotoCount(album.getPhotoCount());
        if (album.getStatus() != null && !album.getStatus().isBlank())
            existing.setStatus(album.getStatus());
        if (album.getDisplayOrder() != null)
            existing.setDisplayOrder(album.getDisplayOrder());
        return galleryAlbumRepository.save(existing);
    }

    public void delete(Long id) {
        GalleryAlbumEntity existing = getById(id);
        galleryAlbumRepository.delete(existing);
    }
}

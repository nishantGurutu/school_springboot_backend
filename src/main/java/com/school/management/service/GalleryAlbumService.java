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
        return galleryAlbumRepository.findByStatusIgnoreCaseOrderByDisplayOrderAsc("Active").stream()
                .filter(GalleryAlbumEntity::hasPhotos).toList();
    }

    public static List<java.util.Map<String, Object>> flattenPhotos(List<GalleryAlbumEntity> albums) {
        List<java.util.Map<String, Object>> photos = new java.util.ArrayList<>();
        for (GalleryAlbumEntity album : albums) {
            List<String> urls = album.getImages();
            if (urls == null || urls.isEmpty()) {
                urls = album.getCoverImage() == null ? List.of() : List.of(album.getCoverImage());
            }
            for (int index = 0; index < urls.size(); index++) {
                String url = urls.get(index);
                if (url == null || url.isBlank()) continue;
                photos.add(java.util.Map.of("id", album.getId() + "-" + index, "imageUrl", url));
            }
        }
        return photos;
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> getActivePhotos() {
        return flattenPhotos(getActiveAlbums());
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
        syncImages(album);
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
        if (album.getImages() != null) {
            existing.setImages(new java.util.ArrayList<>(album.getImages()));
        }
        syncImages(existing);
        return galleryAlbumRepository.save(existing);
    }

    private void syncImages(GalleryAlbumEntity album) {
        if (album.getImages() == null) {
            if (album.hasPhotos()) return; // Preserve legacy cover-only albums.
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Select at least one valid image");
        }
        if (album.getImages().isEmpty() || album.getImages().stream().anyMatch(url -> url == null || url.isBlank())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Select at least one valid image");
        }
        album.setCoverImage(album.getImages().get(0));
        album.setPhotoCount(album.getImages().size());
    }

    public void delete(Long id) {
        GalleryAlbumEntity existing = getById(id);
        galleryAlbumRepository.delete(existing);
    }
}

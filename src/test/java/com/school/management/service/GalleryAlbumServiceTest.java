package com.school.management.service;

import com.school.management.entity.GalleryAlbumEntity;
import com.school.management.repository.GalleryAlbumRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GalleryAlbumServiceTest {
    private final GalleryAlbumRepository repository = mock(GalleryAlbumRepository.class);
    private final GalleryAlbumService service = new GalleryAlbumService(repository);

    @Test void createDerivesCoverAndCountFromAllPhotos() {
        var album = GalleryAlbumEntity.builder().images(List.of("/api/files/media/a", "/api/files/media/b")).photoCount(99).build();
        when(repository.save(album)).thenReturn(album);
        var saved = service.create(album);
        assertEquals(2, saved.getPhotoCount());
        assertEquals("/api/files/media/a", saved.getCoverImage());
        assertEquals("School Album", saved.getTitle());
    }

    @Test void updateReplacesPhotosAndRecalculatesMetadata() {
        var existing = GalleryAlbumEntity.builder().id(1L).coverImage("old").build();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        var saved = service.update(1L, GalleryAlbumEntity.builder().images(List.of("new", "second")).build());
        assertEquals(List.of("new", "second"), saved.getImages());
        assertEquals("new", saved.getCoverImage());
        assertEquals(2, saved.getPhotoCount());
    }

    @Test void rejectsEmptyPhotoSelection() {
        assertThrows(ResponseStatusException.class, () -> service.create(GalleryAlbumEntity.builder().images(List.of()).build()));
        verify(repository, never()).save(any());
    }

    @Test void legacyCoverOnlyAlbumStillWorks() {
        var album = GalleryAlbumEntity.builder().coverImage("legacy").build();
        when(repository.save(album)).thenReturn(album);
        assertEquals("legacy", service.create(album).getCoverImage());
    }
    @Test void oldLocalhostMediaIsReturnedAsPortablePaths() {
        var album = GalleryAlbumEntity.builder()
                .coverImage("http://localhost:8080/api/files/media/abc")
                .images(List.of("http://localhost:8080/api/files/media/abc")).build();
        assertEquals("/api/files/media/abc", album.getCoverImage());
        assertEquals(List.of("/api/files/media/abc"), album.getImages());
    }

    @Test void activeGalleryExcludesEmptyRecords() {
        var empty = GalleryAlbumEntity.builder().coverImage("").images(List.of()).build();
        var photo = GalleryAlbumEntity.builder().coverImage("/api/files/media/abc").build();
        when(repository.findByStatusIgnoreCaseOrderByDisplayOrderAsc("Active")).thenReturn(List.of(empty, photo));
        assertEquals(List.of(photo), service.getActiveAlbums());
    }

    @Test void rejectsNewEmptyLegacyRecord() {
        assertThrows(ResponseStatusException.class, () -> service.create(new GalleryAlbumEntity()));
    }
    @Test void galleryIsOneFlatListWithoutEmptyAlbumsOrNestedImages() {
        var empty = GalleryAlbumEntity.builder().id(1L).coverImage("").images(List.of()).build();
        var batch = GalleryAlbumEntity.builder().id(2L).images(List.of("/api/files/media/a", "/api/files/media/b")).build();
        var legacy = GalleryAlbumEntity.builder().id(3L).coverImage("/api/files/media/c").build();
        var photos = GalleryAlbumService.flattenPhotos(List.of(empty, batch, legacy));
        assertEquals(3, photos.size());
        assertEquals(java.util.Map.of("id", "2-0", "imageUrl", "/api/files/media/a"), photos.get(0));
        assertEquals("/api/files/media/b", photos.get(1).get("imageUrl"));
        assertEquals("/api/files/media/c", photos.get(2).get("imageUrl"));
        assertTrue(photos.stream().noneMatch(photo -> photo.containsKey("images")));
    }
}

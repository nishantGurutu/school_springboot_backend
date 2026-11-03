package com.school.management.service;
import com.school.management.entity.BannerEntity;
import com.school.management.repository.BannerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BannerServiceTest {
    private final BannerRepository repository = mock(BannerRepository.class);
    private final BannerService service = new BannerService(repository);

    @Test void multipleImagesBecomeSeparateOrderedBanners() {
        when(repository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(BannerEntity.builder().displayOrder(3).build()));
        when(repository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));
        var saved = service.createMany(List.of("http://localhost:8080/api/files/media/a", "/api/files/media/b"));
        assertEquals(2, saved.size());
        assertEquals("/api/files/media/a", saved.get(0).getImageUrl());
        assertEquals("/api/files/media/b", saved.get(1).getImageUrl());
        assertEquals(4, saved.get(0).getDisplayOrder());
        assertEquals(5, saved.get(1).getDisplayOrder());
        assertTrue(saved.stream().allMatch(banner -> "Active".equals(banner.getStatus())));
    }
    @Test void invalidBatchSavesNothing() {
        assertThrows(ResponseStatusException.class, () -> service.createMany(List.of("valid", "")));
        assertThrows(ResponseStatusException.class, () -> service.createMany(List.of()));
        verify(repository, never()).saveAll(any());
    }
}

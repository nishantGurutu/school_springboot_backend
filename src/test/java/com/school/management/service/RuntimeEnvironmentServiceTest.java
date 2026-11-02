package com.school.management.service;

import com.school.management.entity.RuntimeEnvironment;
import com.school.management.repository.RuntimeEnvironmentRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuntimeEnvironmentServiceTest {
    final RuntimeEnvironmentRepository repository = mock(RuntimeEnvironmentRepository.class);
    final RuntimeEnvironmentService service = new RuntimeEnvironmentService(repository, "https://live.example", "http://192.168.1.4:8080");
    @Test void defaultsToLiveWithoutWritingOnPublicRead() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertEquals("https://live.example/api", service.active().activeUrl());
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void savesOnlyLiveAndPreservesLegacyDevelopmentUrl() {
        var entity = new RuntimeEnvironment();
        entity.setDevelopmentUrl("http://legacy.example/api");
        entity.setEnvironment("development");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        var saved = service.save(new RuntimeEnvironmentService.Configuration("https://new-live.example/"));
        assertEquals("https://new-live.example/api", saved.liveUrl());
        assertEquals("live", entity.getEnvironment());
        assertEquals("http://legacy.example/api", entity.getDevelopmentUrl());
        verify(repository).saveAndFlush(entity);
    }
    @Test void ignoresPersistedDevelopmentSelection() {
        var entity = new RuntimeEnvironment();
        entity.setLiveUrl("https://live.example/api");
        entity.setDevelopmentUrl("http://legacy.example/api");
        entity.setEnvironment("development");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        assertEquals("live", service.active().environment());
        assertEquals(entity.getLiveUrl(), service.active().activeUrl());
    }
    @Test void rejectsInvalidUrlsWithoutSaving() {
        for (String url : new String[]{"", "ftp://server", "https://user:pass@server", "https://server?x=1", "https://server#x", "localhost:8080"}) {
            assertThrows(ResponseStatusException.class, () -> service.save(new RuntimeEnvironmentService.Configuration(url)));
        }
        verify(repository, never()).saveAndFlush(any());
    }
}

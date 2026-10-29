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
    @Test void savesOneEnvironmentAndBothNormalizedUrls() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        var saved = service.save(new RuntimeEnvironmentService.Configuration("development", "https://live.example/api/", "http://192.168.1.4:8080/"));
        assertEquals("development", saved.environment());
        assertEquals("http://192.168.1.4:8080/api", saved.developmentUrl());
        verify(repository).saveAndFlush(argThat(v -> v.getId() == 1L && v.getEnvironment().equals("development") && v.getLiveUrl().equals("https://live.example/api")));
    }
    @Test void selectsPersistedDevelopmentAndThenLive() {
        var entity = new RuntimeEnvironment();
        entity.setLiveUrl("https://live.example/api"); entity.setDevelopmentUrl("http://192.168.1.4:8080/api");
        entity.setEnvironment("development");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        assertEquals(entity.getDevelopmentUrl(), service.active().activeUrl());
        entity.setEnvironment("live");
        assertEquals(entity.getLiveUrl(), service.active().activeUrl());
    }
    @Test void rejectsInvalidEnvironmentAndUrlsWithoutSaving() {
        assertThrows(ResponseStatusException.class, () -> service.save(new RuntimeEnvironmentService.Configuration("both", "https://live.example", "http://dev.example")));
        for (String url : new String[]{"", "ftp://server", "https://user:pass@server", "https://server?x=1", "https://server#x", "localhost:8080"}) {
            assertThrows(ResponseStatusException.class, () -> service.save(new RuntimeEnvironmentService.Configuration("live", url, "http://dev.example")));
        }
        verify(repository, never()).saveAndFlush(any());
    }
}

package com.school.management.service;
import com.school.management.repository.RuntimeEnvironmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RuntimeEnvironmentPersistenceTest {
    @Autowired RuntimeEnvironmentRepository repository;
    @Test void committedSelectionSurvivesNewServiceDefaultsRepeatedReadsAndExplicitSwitchBack() {
        repository.deleteAll();
        try {
            var first = new RuntimeEnvironmentService(repository, "https://initial-live.example/api", "http://initial-dev.example/api");
            first.save(new RuntimeEnvironmentService.Configuration("development", "https://school.example/api", "http://192.168.1.25:8080/api"));
            assertEquals("development", repository.findById(1L).orElseThrow().getEnvironment());
            var restarted = new RuntimeEnvironmentService(repository, "https://different-live.example/api", "http://different-dev.example/api");
            for (int i = 0; i < 10; i++) {
                assertEquals("development", restarted.read().environment());
                assertEquals("http://192.168.1.25:8080/api", restarted.active().activeUrl());
            }
            assertEquals(1, repository.count());
            restarted.save(new RuntimeEnvironmentService.Configuration("live", "https://school.example/api", "http://192.168.1.25:8080/api"));
            assertEquals("live", first.read().environment());
            assertEquals("https://school.example/api", first.active().activeUrl());
            assertEquals(1, repository.count());
        } finally { repository.deleteAll(); }
    }
    @Test void changedUrlsAreCommittedAndReturnedWhenSettingsReopens() {
        repository.deleteAll();
        try {
            var service = new RuntimeEnvironmentService(repository, "https://default.example/api", "http://default.example/api");
            service.save(new RuntimeEnvironmentService.Configuration("development", "https://first-live.example/api", "http://first-dev.example/api"));
            // Change both fields, including the inactive Live URL, without changing selection.
            var saved = service.save(new RuntimeEnvironmentService.Configuration("development", "https://new-live.example/", "http://192.168.1.80:9090/"));
            var reopened = new RuntimeEnvironmentService(repository, "https://ignored.example/api", "http://ignored.example/api");
            assertEquals(saved, reopened.read());
            assertEquals("https://new-live.example/api", reopened.read().liveUrl());
            assertEquals("http://192.168.1.80:9090/api", reopened.read().developmentUrl());
            assertEquals("development", reopened.read().environment());
            assertEquals(saved.developmentUrl(), reopened.active().activeUrl());
            var row = repository.findById(1L).orElseThrow();
            assertEquals(saved.liveUrl(), row.getLiveUrl());
            assertEquals(saved.developmentUrl(), row.getDevelopmentUrl());
            assertEquals(1, repository.count());
        } finally { repository.deleteAll(); }
    }

}

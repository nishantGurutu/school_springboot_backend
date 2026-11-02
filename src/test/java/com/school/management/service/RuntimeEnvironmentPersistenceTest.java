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
    @Test void liveUrlSurvivesRestartAndSettingsReopen() {
        repository.deleteAll();
        try {
            var service = new RuntimeEnvironmentService(repository, "https://default.example/api", "http://dev.example/api");
            service.save(new RuntimeEnvironmentService.Configuration("https://first.example/api"));
            var saved = service.save(new RuntimeEnvironmentService.Configuration("https://new-live.example/"));
            var reopened = new RuntimeEnvironmentService(repository, "https://ignored.example/api", "http://other-dev.example/api");
            assertEquals(saved, reopened.read());
            assertEquals("https://new-live.example/api", reopened.active().activeUrl());
            assertEquals("live", reopened.active().environment());
            assertEquals(1, repository.count());
        } finally { repository.deleteAll(); }
    }
}

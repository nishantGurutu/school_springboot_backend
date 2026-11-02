package com.school.management.service;

import com.school.management.entity.RuntimeEnvironment;
import com.school.management.repository.RuntimeEnvironmentRepository;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RuntimeEnvironmentService {
    private final RuntimeEnvironmentRepository repository;
    private final String liveDefault;
    private final String developmentDefault;
    public record Configuration(String liveUrl) {}
    public record Active(String environment, String activeUrl) {}
    public RuntimeEnvironmentService(RuntimeEnvironmentRepository repository,
        @Value("${app.runtime.live-url:https://school-springboot-backend.onrender.com/api}") String live,
        @Value("${app.runtime.development-url:http://10.103.180.61:8080/api}") String development) {
        this.repository = repository; this.liveDefault = live; this.developmentDefault = development;
    }
    public Configuration read() {
        return new Configuration(repository.findById(1L).map(RuntimeEnvironment::getLiveUrl)
            .orElseGet(() -> normalize(liveDefault)));
    }
    public Active active() {
        // Development selection belongs to each client's build configuration.
        return new Active("live", read().liveUrl());
    }
    @Transactional
    public Configuration save(Configuration c) {
        if (c == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a live URL");
        String live = normalize(c.liveUrl());
        var entity = repository.findById(1L).orElseGet(RuntimeEnvironment::new);
        entity.setEnvironment("live");
        entity.setLiveUrl(live);
        // Retain the legacy column for schema compatibility; clients never use it.
        if (entity.getDevelopmentUrl() == null) entity.setDevelopmentUrl(normalize(developmentDefault));
        repository.saveAndFlush(entity);
        return new Configuration(live);
    }
    static String normalize(String value) {
        try {
            URI uri = URI.create(value == null ? "" : value.trim());
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
                throw new IllegalArgumentException();
            String url = uri.toString().replaceAll("/+$", "");
            return url.endsWith("/api") ? url : url + "/api";
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid HTTP(S) API URL without credentials, query or fragment");
        }
    }
}

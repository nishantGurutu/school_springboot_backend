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
    public record Configuration(String environment, String liveUrl, String developmentUrl) {}
    public record Active(String environment, String activeUrl) {}
    public RuntimeEnvironmentService(RuntimeEnvironmentRepository repository,
        @Value("${app.runtime.live-url:https://school-springboot-backend.onrender.com/api}") String live,
        @Value("${app.runtime.development-url:http://10.103.180.61:8080/api}") String development) {
        this.repository = repository; this.liveDefault = live; this.developmentDefault = development;
    }
    public Configuration read() {
        return repository.findById(1L).map(v -> new Configuration(v.getEnvironment(), v.getLiveUrl(), v.getDevelopmentUrl()))
            .orElseGet(() -> new Configuration("live", normalize(liveDefault), normalize(developmentDefault)));
    }
    public Active active() {
        var c = read();
        return new Active(c.environment(), "live".equals(c.environment()) ? c.liveUrl() : c.developmentUrl());
    }
    @Transactional
    public Configuration save(Configuration c) {
        if (c == null || !("live".equals(c.environment()) || "development".equals(c.environment())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select live or development");
        String live = normalize(c.liveUrl()), dev = normalize(c.developmentUrl());
        var entity = repository.findById(1L).orElseGet(RuntimeEnvironment::new);
        entity.setEnvironment(c.environment()); entity.setLiveUrl(live); entity.setDevelopmentUrl(dev);
        repository.saveAndFlush(entity);
        return new Configuration(c.environment(), live, dev);
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

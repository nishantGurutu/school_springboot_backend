package com.school.management.service;

import com.school.management.entity.PushDeviceEntity;
import com.school.management.repository.PushDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;

@Service @RequiredArgsConstructor @Transactional
public class PushDeviceService {
    private final PushDeviceRepository devices;
    private String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    public void register(Long userId, String token) {
        if (token == null || token.isBlank()) return;
        token = token.trim();
        if (token.length() < 20 || token.length() > 4096)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid device token");
        devices.save(PushDeviceEntity.builder().id(hash(token)).token(token).userId(userId).build());
    }
    public void remove(Long userId, String token) {
        if (token == null || token.isBlank()) return;
        devices.findById(hash(token.trim())).filter(d -> Objects.equals(d.getUserId(), userId)).ifPresent(devices::delete);
    }
}

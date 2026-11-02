package com.school.management.controller;

import com.school.management.service.MediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController @RequestMapping("/api/files/media") @RequiredArgsConstructor
public class MediaController {
    private final MediaStorageService media;
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String,Object>> upload(@RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "photo") String kind) {
        var saved = media.save(file, kind);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", saved.getId(),
                "fileUrl", "/api/files/media/" + saved.getId(), "fileName", saved.getFileName(),
                "contentType", saved.getContentType(), "fileSizeBytes", saved.getSizeBytes()));
    }
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> read(@PathVariable String id, @RequestParam(defaultValue="false") boolean attachment) {
        var saved = media.get(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(saved.getContentType()))
                .contentLength(saved.getSizeBytes())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, ((attachment || (!saved.getContentType().startsWith("image/") && !saved.getContentType().equals("application/pdf"))) ? ContentDisposition.attachment() : ContentDisposition.inline())
                        .filename(saved.getFileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff").body(saved.getContent());
    }
}

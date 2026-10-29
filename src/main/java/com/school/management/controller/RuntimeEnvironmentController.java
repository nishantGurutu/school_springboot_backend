package com.school.management.controller;

import com.school.management.service.RuntimeEnvironmentService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class RuntimeEnvironmentController {
    private final RuntimeEnvironmentService service;
    public RuntimeEnvironmentController(RuntimeEnvironmentService service) { this.service = service; }
    @GetMapping("/api/runtime-config")
    public ResponseEntity<RuntimeEnvironmentService.Active> active() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.active());
    }
    @GetMapping("/api/admin/runtime-config")
    public ResponseEntity<RuntimeEnvironmentService.Configuration> read() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.read());
    }
    @PutMapping("/api/admin/runtime-config")
    public RuntimeEnvironmentService.Configuration save(@RequestBody RuntimeEnvironmentService.Configuration configuration) {
        return service.save(configuration);
    }
}

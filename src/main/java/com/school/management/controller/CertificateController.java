package com.school.management.controller;

import com.school.management.dto.CertificateRequest;
import com.school.management.dto.CertificateResponse;
import com.school.management.service.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
@Tag(name = "Certificates", description = "Certificate management APIs")
public class CertificateController {

    private final CertificateService certificateService;

    @PostMapping
    @Operation(summary = "Create a new certificate")
    public ResponseEntity<CertificateResponse> create(@Valid @RequestBody CertificateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(certificateService.create(request));
    }

    @GetMapping
    @Operation(summary = "List all certificates")
    public ResponseEntity<List<CertificateResponse>> getAll() {
        return ResponseEntity.ok(certificateService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get certificate by id")
    public ResponseEntity<CertificateResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(certificateService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a certificate")
    public ResponseEntity<CertificateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CertificateRequest request) {
        return ResponseEntity.ok(certificateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a certificate")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        certificateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.school.management.controller;

import com.school.management.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Storage", description = "Endpoints for uploading and serving curriculum notes and attachments")
public class FileStorageController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload curriculum note or document")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        FileStorageService.UploadResult result = fileStorageService.storeFile(file, "notes");
        return ResponseEntity.ok(Map.of(
                "fileName", result.originalFileName(),
                "storedFileName", result.storedFileName(),
                "fileUrl", result.downloadUrl(),
                "fileType", result.fileType(),
                "fileSizeBytes", result.fileSizeBytes(),
                "fileSizeFormatted", result.fileSizeFormatted()
        ));
    }

    @GetMapping("/download/{fileName:.+}")
    @Operation(summary = "Download or stream uploaded file")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName,
                                                 @RequestParam(required = false, defaultValue = "false") boolean attachment,
                                                 HttpServletRequest request) {
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("Could not determine file type.");
        }
        if (contentType == null) {
            if (fileName.toLowerCase().endsWith(".pdf")) contentType = "application/pdf";
            else if (fileName.toLowerCase().endsWith(".png")) contentType = "image/png";
            else if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) contentType = "image/jpeg";
            else contentType = "application/octet-stream";
        }

        String disposition = attachment ? "attachment; filename=\"" + resource.getFilename() + "\""
                : "inline; filename=\"" + resource.getFilename() + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(resource);
    }
}

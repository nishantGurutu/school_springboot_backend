package com.school.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            Files.createDirectories(this.fileStorageLocation.resolve("notes"));
        } catch (Exception ex) {
            log.error("Could not create the upload directory: {}", ex.getMessage());
        }
    }

    public UploadResult storeFile(MultipartFile file, String subDirectory) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf");
        try {
            if (originalFileName.contains("..")) {
                throw new IllegalArgumentException("Invalid file path sequence: " + originalFileName);
            }

            String extension = "";
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalFileName.substring(dotIndex);
            }

            String storedFileName = UUID.randomUUID().toString() + extension;
            Path targetLocation = this.fileStorageLocation.resolve(subDirectory).resolve(storedFileName);
            Files.createDirectories(targetLocation.getParent());
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            long bytes = file.getSize();
            String formattedSize = formatFileSize(bytes);
            String fileType = resolveFileType(extension);

            return new UploadResult(
                    originalFileName,
                    storedFileName,
                    "/api/files/download/" + storedFileName,
                    fileType,
                    bytes,
                    formattedSize
            );
        } catch (IOException ex) {
            log.error("Failed to store file: {}", ex.getMessage());
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            // Check direct or subfolders (e.g. notes/)
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            if (!Files.exists(filePath)) {
                filePath = this.fileStorageLocation.resolve("notes").resolve(fileName).normalize();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found or not readable: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File path malformed: " + fileName, ex);
        }
    }

    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 KB";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    public static String resolveFileType(String extension) {
        if (extension == null) return "DOC";
        String ext = extension.toLowerCase().replace(".", "");
        return switch (ext) {
            case "pdf" -> "PDF";
            case "doc", "docx" -> "DOC";
            case "ppt", "pptx" -> "PPT";
            case "xls", "xlsx" -> "EXCEL";
            case "png", "jpg", "jpeg", "webp" -> "IMAGE";
            case "txt" -> "TXT";
            default -> "FILE";
        };
    }

    public record UploadResult(
            String originalFileName,
            String storedFileName,
            String downloadUrl,
            String fileType,
            long fileSizeBytes,
            String fileSizeFormatted
    ) {}
}

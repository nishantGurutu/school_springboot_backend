package com.school.management.service;

import com.school.management.entity.StoredMediaEntity;
import com.school.management.repository.StoredMediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.Instant;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class MediaStorageService {
    private static final long MAX_BYTES = 8 * 1024 * 1024;
    private final StoredMediaRepository repository;

    @Transactional
    public StoredMediaEntity save(MultipartFile file, String kind) {
        if (file == null || file.isEmpty()) throw bad("Choose a non-empty image or document");
        if (file.getSize() > MAX_BYTES) throw bad("Choose a file up to 8 MB");
        if (!"photo".equals(kind) && !"document".equals(kind)) throw bad("Invalid upload type");
        try {
            byte[] input = file.getBytes();
            byte[] stored;
            String mime;
            String name = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._ -]", "_");
            if (name.length() > 140) name = name.substring(name.length() - 140);
            boolean pdf = input.length >= 5 && new String(input, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-");
            if ("document".equals(kind) && pdf) {
                stored = input; mime = "application/pdf";
                if (!name.toLowerCase().endsWith(".pdf")) name += ".pdf";
            } else if ("document".equals(kind) && isWordDocument(input, name)) {
                stored = input;
                mime = name.toLowerCase().endsWith(".docx") ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document" : "application/msword";
            } else {
                stored = compress(input, "document".equals(kind) ? 2200 : 1600, "document".equals(kind) ? .90f : .86f);
                mime = "image/jpeg";
                name = name.replaceFirst("\\.[^.]+$", "") + ".jpg";
            }
            StoredMediaEntity media = new StoredMediaEntity();
            media.setId(UUID.randomUUID().toString()); media.setFileName(name);
            media.setContentType(mime); media.setSizeBytes(stored.length);
            media.setCreatedAt(Instant.now()); media.setContent(stored);
            return repository.save(media);
        } catch (IOException e) { throw bad("Unable to read this file. Choose a JPEG, PNG, PDF or Word document."); }
    }

    private boolean isWordDocument(byte[] bytes, String name) throws IOException {
        if (name.toLowerCase().endsWith(".doc") && bytes.length >= 8) {
            byte[] signature = {(byte)0xd0,(byte)0xcf,0x11,(byte)0xe0,(byte)0xa1,(byte)0xb1,0x1a,(byte)0xe1};
            return java.util.Arrays.equals(signature, java.util.Arrays.copyOf(bytes,8));
        }
        if (!name.toLowerCase().endsWith(".docx")) return false;
        boolean types = false, document = false;
        long expanded = 0; int entries = 0;
        try (var zip = new java.util.zip.ZipInputStream(new ByteArrayInputStream(bytes))) {
            java.util.zip.ZipEntry entry; byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > 500) throw bad("Document contains too many embedded files");
                if (entry.getName().toLowerCase().endsWith("vbaproject.bin")) throw bad("Upload a document without macros");
                types |= entry.getName().equals("[Content_Types].xml");
                document |= entry.getName().equals("word/document.xml");
                int count;
                while ((count = zip.read(buffer)) != -1) {
                    expanded += count;
                    if (expanded > 20 * 1024 * 1024) throw bad("Document expanded size is too large");
                }
            }
        }
        return types && document;
    }

    private byte[] compress(byte[] bytes, int maxEdge, float quality) throws IOException {
        BufferedImage original;
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw bad("Choose a supported image (JPEG or PNG), or a PDF / Word document");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long)width * height > 24_000_000L) throw bad("Image is too large; use an image below 24 megapixels");
                original = reader.read(0);
            } finally { reader.dispose(); }
        }
        double scale = Math.min(1, (double)maxEdge / Math.max(original.getWidth(), original.getHeight()));
        BufferedImage resized = new BufferedImage(Math.max(1, (int)Math.round(original.getWidth()*scale)), Math.max(1, (int)Math.round(original.getHeight()*scale)), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, resized.getWidth(), resized.getHeight());
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(original, 0, 0, resized.getWidth(), resized.getHeight(), null);
        } finally { graphics.dispose(); }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (var stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream);
            var params = writer.getDefaultWriteParam(); params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); params.setCompressionQuality(quality);
            writer.write(null, new IIOImage(resized, null, null), params);
        } finally { writer.dispose(); original.flush(); resized.flush(); }
        return output.toByteArray();
    }

    @Transactional(readOnly = true)
    public StoredMediaEntity get(String id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image / document not found"));
    }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}

package com.school.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "gallery_albums", indexes = {
        @Index(name = "idx_gallery_status", columnList = "status"),
        @Index(name = "idx_gallery_display_order", columnList = "display_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GalleryAlbumEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "gallery_album_images", joinColumns = @JoinColumn(name = "album_id"))
    @OrderColumn(name = "image_order")
    @Column(name = "image_url", length = 2000, nullable = false)
    private java.util.List<String> images;

    @Builder.Default
    @Column(name = "photo_count")
    private Integer photoCount = 1;

    @Builder.Default
    @Column(nullable = false, length = 16)
    private String status = "Active";

    @Builder.Default
    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Store/return first-party media as paths, independent of the uploading host.
    public static String mediaPath(String value) {
        if (value == null) return null;
        try {
            String path = java.net.URI.create(value).getPath();
            if (path != null && path.startsWith("/api/files/media/")) return path;
        } catch (IllegalArgumentException ignored) { }
        return value;
    }

    public String getCoverImage() {
        return mediaPath(coverImage);
    }

    public java.util.List<String> getImages() {
        return images == null ? null : images.stream().map(GalleryAlbumEntity::mediaPath).toList();
    }

    public boolean hasPhotos() {
        return (images != null && images.stream().anyMatch(url -> url != null && !url.isBlank()))
                || (coverImage != null && !coverImage.isBlank());
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (displayOrder == null) displayOrder = 0;
        if (photoCount == null) photoCount = 1;
        if (status == null || status.isBlank()) status = "Active";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

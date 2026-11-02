package com.school.management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "stored_media")
@Getter @Setter @NoArgsConstructor
public class StoredMediaEntity {
    @Id @Column(length = 36) private String id;
    @Column(nullable = false, length = 160) private String fileName;
    @Column(nullable = false, length = 100) private String contentType;
    @Column(nullable = false) private long sizeBytes;
    @Column(nullable = false) private Instant createdAt;
    // PostgreSQL bytea avoids base64 expansion and Hibernate's large-object OID storage.
    @Column(nullable = false, columnDefinition = "bytea") private byte[] content;
}

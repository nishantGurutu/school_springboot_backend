package com.school.management.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "runtime_environment")
@Getter @Setter
public class RuntimeEnvironment {
    @Id private Long id = 1L;
    private String environment = "live";
    @Column(length = 2048) private String liveUrl;
    @Column(length = 2048) private String developmentUrl;
    @Version private Long version;
}

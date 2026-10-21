package com.school.management.dto.transport;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyChecklistItemDto {
    private Long id;
    private String schoolId;
    private String title;
    private String description;
    private String iconKey;
    private Boolean isRequired; // also serialized as isRequired
    private Boolean required;   // convenience alias for frontend / mobile
    private Boolean isActive;
    private Boolean active;     // convenience alias
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Boolean getRequired() {
        return required != null ? required : isRequired;
    }

    public Boolean getActive() {
        return active != null ? active : isActive;
    }
}

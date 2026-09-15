package com.school.management.dto;

import com.school.management.domain.guardian.GuardianType;
import com.school.management.entity.GuardianEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GuardianResponse {

    // --- Dashboard table columns ---
    private Long id;
    private String name;             // Guardian Name
    private GuardianType relation;   // Relation (Father/Mother/...)
    private String studentName;      // displayed ward's name
    private String occupation;
    private String phone;
    private String email;
    private String address;
    private String feeStatus;

    // --- Full details ---
    private String guardianType;
    private String photo;
    private String studentAdmissionNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GuardianResponse fromEntity(GuardianEntity g) {
        return fromEntity(g, null);
    }

    public static GuardianResponse fromEntity(GuardianEntity g, String studentName) {
        return GuardianResponse.builder()
                .id(g.getId())
                .name(g.getName())
                .relation(g.getGuardianType())
                .studentName(studentName)
                .occupation(g.getOccupation())
                .phone(g.getPhone())
                .email(g.getEmail())
                .address(g.getAddress())
                .feeStatus(g.getFeeStatus())
                .guardianType(g.getGuardianType() == null ? null : g.getGuardianType().name())
                .photo(g.getPhoto())
                .studentAdmissionNo(g.getStudentAdmissionNo())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }
}
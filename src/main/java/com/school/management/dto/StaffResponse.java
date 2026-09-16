package com.school.management.dto;

import com.school.management.entity.StaffEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffResponse {

    private Long id;
    private String name;
    private String staffType;
    private String designation;
    private String phone;
    private String email;
    private Double salary;
    private String joinDate;
    private String status;

    public static StaffResponse fromEntity(StaffEntity staff) {
        return StaffResponse.builder()
                .id(staff.getId())
                .name(staff.getName())
                .staffType(staff.getStaffType())
                .designation(staff.getDesignation())
                .phone(staff.getPhone())
                .email(staff.getEmail())
                .salary(staff.getSalary())
                .joinDate(staff.getJoinDate())
                .status(staff.getStatus())
                .build();
    }
}
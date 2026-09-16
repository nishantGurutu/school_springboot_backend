package com.school.management.dto;

import com.school.management.entity.LeaveRequestEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeaveRequestResponse {

    private Long id;
    private String applyDate;
    private String name;
    private String userType;
    private String leaveType;
    private String date;
    private String duration;
    private String status;
    private String reason;
    private String note;

    public static LeaveRequestResponse fromEntity(LeaveRequestEntity entity) {
        return LeaveRequestResponse.builder()
                .id(entity.getId())
                .applyDate(entity.getApplyDate())
                .name(entity.getName())
                .userType(entity.getUserType())
                .leaveType(entity.getLeaveType())
                .date(entity.getDate())
                .duration(entity.getDuration())
                .status(entity.getStatus())
                .reason(entity.getReason())
                .note(entity.getNote())
                .build();
    }
}

package com.school.management.dto;

import com.school.management.entity.ClassRoomEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClassRoomResponse {

    private Long id;
    private String room;
    private String capacity;
    private String status;

    public static ClassRoomResponse fromEntity(ClassRoomEntity entity) {
        return ClassRoomResponse.builder()
                .id(entity.getId())
                .room(entity.getRoom())
                .capacity(entity.getCapacity())
                .status(entity.getStatus())
                .build();
    }
}

package com.school.management.dto;

import com.school.management.entity.NoticeEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NoticeResponse {

    private Long id;
    private String title;
    private String date;
    private String target;
    private String category;

    public static NoticeResponse fromEntity(NoticeEntity notice) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .date(notice.getDate())
                .target(notice.getTarget())
                .category(notice.getCategory())
                .build();
    }
}
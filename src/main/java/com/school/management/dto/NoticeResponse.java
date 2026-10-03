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
    private String content;
    private String createdAt;

    public static NoticeResponse fromEntity(NoticeEntity notice) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .date(notice.getDate() != null && !notice.getDate().isBlank() 
                        ? notice.getDate() 
                        : (notice.getCreatedAt() != null ? notice.getCreatedAt().toLocalDate().toString() : java.time.LocalDate.now().toString()))
                .target(notice.getTarget() != null ? notice.getTarget() : "ALL")
                .category(notice.getCategory() != null ? notice.getCategory() : "General")
                .content(notice.getContent())
                .createdAt(notice.getCreatedAt() != null ? notice.getCreatedAt().toString() : null)
                .build();
    }
}
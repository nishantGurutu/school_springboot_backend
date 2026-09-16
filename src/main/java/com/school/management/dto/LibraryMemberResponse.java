package com.school.management.dto;

import com.school.management.entity.LibraryMemberEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LibraryMemberResponse {

    private Long id;
    private String joinDate;
    private String cardNo;
    private String studentName;
    private String className;
    private String section;
    private String phone;
    private String email;
    private String gender;
    private String bookIssue;
    private String issueDate;
    private String returnDate;
    private String avatar;

    public static LibraryMemberResponse fromEntity(LibraryMemberEntity member) {
        return LibraryMemberResponse.builder()
                .id(member.getId())
                .joinDate(member.getJoinDate())
                .cardNo(member.getCardNo())
                .studentName(member.getStudentName())
                .className(member.getClassName())
                .section(member.getSection())
                .phone(member.getPhone())
                .email(member.getEmail())
                .gender(member.getGender())
                .bookIssue(member.getBookIssue())
                .issueDate(member.getIssueDate())
                .returnDate(member.getReturnDate())
                .avatar(member.getAvatar())
                .build();
    }
}
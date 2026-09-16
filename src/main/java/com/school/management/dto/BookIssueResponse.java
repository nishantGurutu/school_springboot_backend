package com.school.management.dto;

import com.school.management.entity.BookIssueEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookIssueResponse {

    private Long id;
    private String cardNo;
    private String issueTo;
    private String className;
    private String bookName;
    private String number;
    private String issueDate;
    private String returnDate;
    private String status;

    public static BookIssueResponse fromEntity(BookIssueEntity issue) {
        return BookIssueResponse.builder()
                .id(issue.getId())
                .cardNo(issue.getCardNo())
                .issueTo(issue.getIssueTo())
                .className(issue.getClassName())
                .bookName(issue.getBookName())
                .number(issue.getNumber())
                .issueDate(issue.getIssueDate())
                .returnDate(issue.getReturnDate())
                .status(issue.getStatus())
                .build();
    }
}
package com.school.management.dto;

import com.school.management.entity.BookEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookResponse {

    private Long id;
    private String subject;
    private String name;
    private String publisher;
    private String author;
    private String number;
    private String rackNo;
    private Integer qty;
    private Integer available;
    private String price;
    private String date;

    public static BookResponse fromEntity(BookEntity book) {
        return BookResponse.builder()
                .id(book.getId())
                .subject(book.getSubject())
                .name(book.getName())
                .publisher(book.getPublisher())
                .author(book.getAuthor())
                .number(book.getBookNumber())
                .rackNo(book.getRackNo())
                .qty(book.getQty())
                .available(book.getAvailable())
                .price(book.getPrice())
                .date(book.getDate())
                .build();
    }
}
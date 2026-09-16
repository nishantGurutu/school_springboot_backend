package com.school.management.service;

import com.school.management.dto.BookIssueRequest;
import com.school.management.dto.BookIssueResponse;
import com.school.management.entity.BookIssueEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.BookIssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookIssueService {

    private final BookIssueRepository bookIssueRepository;

    @Transactional(readOnly = true)
    public List<BookIssueResponse> getAll() {
        return bookIssueRepository.findAll().stream().map(BookIssueResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public BookIssueResponse getById(Long id) {
        return BookIssueResponse.fromEntity(findByIdOrThrow(id));
    }

    @Transactional
    public BookIssueResponse create(BookIssueRequest request) {
        if (request.getCardNo() != null && request.getBookName() != null
                && bookIssueRepository.existsByCardNoAndBookName(request.getCardNo().trim(), request.getBookName().trim())) {
            throw new DuplicateResourceException(
                    "Book '" + request.getBookName() + "' already issued to card number '" + request.getCardNo() + "'");
        }
        BookIssueEntity issue = mapToEntity(new BookIssueEntity(), request, true);
        return BookIssueResponse.fromEntity(bookIssueRepository.save(issue));
    }

    @Transactional
    public BookIssueResponse update(Long id, BookIssueRequest request) {
        BookIssueEntity issue = findByIdOrThrow(id);
        return BookIssueResponse.fromEntity(bookIssueRepository.save(mapToEntity(issue, request, false)));
    }

    @Transactional
    public void delete(Long id) {
        BookIssueEntity issue = findByIdOrThrow(id);
        bookIssueRepository.delete(issue);
    }

    private BookIssueEntity mapToEntity(BookIssueEntity e, BookIssueRequest r, boolean create) {
        e.setCardNo(r.getCardNo());
        e.setIssueTo(requireNonBlank(r.getIssueTo(), "Issue to is required").trim());
        e.setClassName(r.getClassName());
        e.setBookName(requireNonBlank(r.getBookName(), "Book name is required").trim());
        e.setNumber(r.getNumber());
        e.setIssueDate(r.getIssueDate());
        e.setReturnDate(r.getReturnDate());
        e.setStatus(r.getStatus() == null || r.getStatus().isBlank() ? "Issued" : r.getStatus().trim());
        return e;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private BookIssueEntity findByIdOrThrow(Long id) {
        return bookIssueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book issue not found with id: " + id));
    }
}
package com.school.management.service;

import com.school.management.dto.BookRequest;
import com.school.management.dto.BookResponse;
import com.school.management.entity.BookEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public List<BookResponse> getAll() {
        return bookRepository.findAll().stream().map(BookResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public BookResponse getById(Long id) {
        return BookResponse.fromEntity(findByIdOrThrow(id));
    }

    @Transactional
    public BookResponse create(BookRequest request) {
        if (request.getName() != null && bookRepository.existsByName(request.getName().trim())) {
            throw new DuplicateResourceException(
                    "Book with name '" + request.getName() + "' already exists");
        }
        if (request.getBookNumber() != null && bookRepository.existsByBookNumber(request.getBookNumber().trim())) {
            throw new DuplicateResourceException(
                    "Book with number '" + request.getBookNumber() + "' already exists");
        }
        BookEntity book = mapToEntity(new BookEntity(), request, true);
        return BookResponse.fromEntity(bookRepository.save(book));
    }

    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        BookEntity book = findByIdOrThrow(id);
        return BookResponse.fromEntity(bookRepository.save(mapToEntity(book, request, false)));
    }

    @Transactional
    public void delete(Long id) {
        BookEntity book = findByIdOrThrow(id);
        bookRepository.delete(book);
    }

    private BookEntity mapToEntity(BookEntity e, BookRequest r, boolean create) {
        e.setSubject(r.getSubject());
        e.setName(requireNonBlank(r.getName(), "Book name is required").trim());
        e.setPublisher(r.getPublisher());
        e.setAuthor(r.getAuthor());
        if (create) {
            e.setBookNumber(r.getBookNumber() == null ? null : r.getBookNumber().trim());
        }
        e.setRackNo(r.getRackNo());
        e.setQty(r.getQty());
        e.setAvailable(r.getAvailable());
        e.setPrice(normalizePrice(r.getPrice()));
        e.setDate(r.getDate());
        return e;
    }

    private String normalizePrice(String price) {
        if (price == null || price.isBlank()) {
            return price;
        }
        String value = price.trim();
        if (!value.startsWith("$")) {
            value = "$" + value;
        }
        return value;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private BookEntity findByIdOrThrow(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }
}
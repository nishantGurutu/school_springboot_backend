package com.school.management.service;

import com.school.management.dto.LibraryMemberRequest;
import com.school.management.dto.LibraryMemberResponse;
import com.school.management.entity.LibraryMemberEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.LibraryMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LibraryMemberService {

    private final LibraryMemberRepository libraryMemberRepository;

    @Transactional(readOnly = true)
    public List<LibraryMemberResponse> getAll() {
        return libraryMemberRepository.findAll().stream().map(LibraryMemberResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public LibraryMemberResponse getById(Long id) {
        return LibraryMemberResponse.fromEntity(findByIdOrThrow(id));
    }

    @Transactional(readOnly = true)
    public LibraryMemberResponse getByCardNo(String cardNo) {
        LibraryMemberEntity member = libraryMemberRepository.findByCardNo(requireNonBlank(cardNo, "Card number is required").trim())
                .orElseThrow(() -> new ResourceNotFoundException("Library member not found with card number: " + cardNo));
        return LibraryMemberResponse.fromEntity(member);
    }

    @Transactional
    public LibraryMemberResponse create(LibraryMemberRequest request) {
        if (request.getCardNo() != null && libraryMemberRepository.existsByCardNo(request.getCardNo().trim())) {
            throw new DuplicateResourceException(
                    "Library member with card number '" + request.getCardNo() + "' already exists");
        }
        LibraryMemberEntity member = mapToEntity(new LibraryMemberEntity(), request, true);
        return LibraryMemberResponse.fromEntity(libraryMemberRepository.save(member));
    }

    @Transactional
    public LibraryMemberResponse update(Long id, LibraryMemberRequest request) {
        LibraryMemberEntity member = findByIdOrThrow(id);
        return LibraryMemberResponse.fromEntity(libraryMemberRepository.save(mapToEntity(member, request, false)));
    }

    @Transactional
    public void delete(Long id) {
        LibraryMemberEntity member = findByIdOrThrow(id);
        libraryMemberRepository.delete(member);
    }

    private LibraryMemberEntity mapToEntity(LibraryMemberEntity e, LibraryMemberRequest r, boolean create) {
        if (create) {
            e.setCardNo(requireNonBlank(r.getCardNo(), "Card number is required").trim());
        }
        e.setStudentName(requireNonBlank(r.getStudentName(), "Student name is required").trim());
        e.setJoinDate(r.getJoinDate());
        e.setClassName(r.getClassName());
        e.setSection(r.getSection());
        e.setPhone(r.getPhone());
        e.setEmail(r.getEmail());
        e.setGender(r.getGender());
        e.setBookIssue(r.getBookIssue());
        e.setIssueDate(r.getIssueDate());
        e.setReturnDate(r.getReturnDate());
        e.setAvatar(r.getAvatar());
        return e;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private LibraryMemberEntity findByIdOrThrow(Long id) {
        return libraryMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Library member not found with id: " + id));
    }
}
package com.school.management.repository;

import com.school.management.entity.BookIssueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookIssueRepository extends JpaRepository<BookIssueEntity, Long> {

    boolean existsByCardNoAndBookName(String cardNo, String bookName);
}
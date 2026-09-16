package com.school.management.repository;

import com.school.management.entity.BookEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {

    boolean existsByName(String name);

    boolean existsByBookNumber(String bookNumber);
}
package com.school.management.repository;

import com.school.management.entity.LibraryMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LibraryMemberRepository extends JpaRepository<LibraryMemberEntity, Long> {

    Optional<LibraryMemberEntity> findByCardNo(String cardNo);

    boolean existsByCardNo(String cardNo);
}
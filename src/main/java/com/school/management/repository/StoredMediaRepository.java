package com.school.management.repository;
import com.school.management.entity.StoredMediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StoredMediaRepository extends JpaRepository<StoredMediaEntity, String> {}

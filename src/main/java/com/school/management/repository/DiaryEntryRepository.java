package com.school.management.repository;
import com.school.management.entity.DiaryEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DiaryEntryRepository extends JpaRepository<DiaryEntryEntity,Long> { List<DiaryEntryEntity> findAllByOrderByIdDesc(); }

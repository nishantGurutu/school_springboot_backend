package com.school.management.repository;

import com.school.management.entity.BannerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<BannerEntity, Long> {
    List<BannerEntity> findByStatusIgnoreCaseOrderByDisplayOrderAsc(String status);
    List<BannerEntity> findAllByOrderByDisplayOrderAsc();
}

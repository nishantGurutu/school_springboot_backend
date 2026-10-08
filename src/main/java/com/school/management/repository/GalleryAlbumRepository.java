package com.school.management.repository;

import com.school.management.entity.GalleryAlbumEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GalleryAlbumRepository extends JpaRepository<GalleryAlbumEntity, Long> {
    List<GalleryAlbumEntity> findByStatusIgnoreCaseOrderByDisplayOrderAsc(String status);
    List<GalleryAlbumEntity> findAllByOrderByDisplayOrderAsc();
    List<GalleryAlbumEntity> findAllByOrderByIdDesc();
}

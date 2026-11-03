package com.school.management.service;

import com.school.management.entity.BannerEntity;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BannerService {

    private final BannerRepository bannerRepository;

    @Transactional(readOnly = true)
    public List<BannerEntity> getAll() {
        return bannerRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<BannerEntity> getActiveBanners() {
        return bannerRepository.findByStatusIgnoreCaseOrderByDisplayOrderAsc("Active");
    }

    @Transactional(readOnly = true)
    public BannerEntity getById(Long id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with id: " + id));
    }

    public BannerEntity create(BannerEntity banner) {
        if (banner.getTitle() == null || banner.getTitle().isBlank()) {
            banner.setTitle("School Announcement");
        }
        if (banner.getStatus() == null || banner.getStatus().isBlank()) {
            banner.setStatus("Active");
        }
        return bannerRepository.save(banner);
    }

    public List<BannerEntity> createMany(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty() || imageUrls.stream().anyMatch(url -> url == null || url.isBlank())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Select at least one valid banner image");
        }
        int nextOrder = bannerRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(BannerEntity::getDisplayOrder).filter(java.util.Objects::nonNull)
                .max(Integer::compareTo).orElse(0) + 1;
        List<BannerEntity> banners = new java.util.ArrayList<>();
        for (String imageUrl : imageUrls) {
            banners.add(BannerEntity.builder().title("School Banner").status("Active")
                    .imageUrl(com.school.management.entity.GalleryAlbumEntity.mediaPath(imageUrl))
                    .displayOrder(nextOrder++).build());
        }
        return bannerRepository.saveAll(banners);
    }

    public BannerEntity update(Long id, BannerEntity banner) {
        BannerEntity existing = getById(id);
        if (banner.getTitle() != null && !banner.getTitle().isBlank()) existing.setTitle(banner.getTitle());
        if (banner.getSubtitle() != null) existing.setSubtitle(banner.getSubtitle());
        if (banner.getBadge() != null) existing.setBadge(banner.getBadge());
        if (banner.getButtonText() != null) existing.setButtonText(banner.getButtonText());
        if (banner.getWebsiteUrl() != null) existing.setWebsiteUrl(banner.getWebsiteUrl());
        if (banner.getImageUrl() != null) existing.setImageUrl(banner.getImageUrl());
        if (banner.getBgColorHex() != null) existing.setBgColorHex(banner.getBgColorHex());
        if (banner.getStatus() != null && !banner.getStatus().isBlank()) existing.setStatus(banner.getStatus());
        if (banner.getDisplayOrder() != null) existing.setDisplayOrder(banner.getDisplayOrder());
        return bannerRepository.save(existing);
    }

    public void delete(Long id) {
        BannerEntity existing = getById(id);
        bannerRepository.delete(existing);
    }
}

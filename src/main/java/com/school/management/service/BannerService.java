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

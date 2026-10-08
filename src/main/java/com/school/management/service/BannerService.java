package com.school.management.service;

import com.school.management.entity.BannerEntity;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.repository.BannerRepository;
import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    public void seedInitialBannersIfEmpty() {
        try {
            if (bannerRepository.count() == 0) {
                log.info("Seeding initial promotional banners...");
                List<BannerEntity> initialBanners = List.of(
                        BannerEntity.builder()
                                .title("2025/2026\nSCHOOL ADMISSION")
                                .subtitle("Online Registration • Enroll Now")
                                .badge("eSchool Admission")
                                .buttonText("Learn More")
                                .websiteUrl("www.yourschoolwebsite.com")
                                .imageUrl("https://images.unsplash.com/photo-1577896851231-70ef18881754?w=800&auto=format&fit=crop&q=80")
                                .bgColorHex("#EAB308")
                                .status("Active")
                                .displayOrder(1)
                                .build(),
                        BannerEntity.builder()
                                .title("Annual Science & Tech Fair")
                                .subtitle("Showcase Your Innovations")
                                .badge("Upcoming Event")
                                .buttonText("Explore Projects")
                                .websiteUrl("www.yourschoolwebsite.com/science")
                                .imageUrl("https://images.unsplash.com/photo-1503676260728-1c00da094a0b?w=800&auto=format&fit=crop&q=80")
                                .bgColorHex("#3B82F6")
                                .status("Active")
                                .displayOrder(2)
                                .build(),
                        BannerEntity.builder()
                                .title("Inter-School Sports Meet 2026")
                                .subtitle("Track & Athletics Championship")
                                .badge("Sports League")
                                .buttonText("View Schedule")
                                .websiteUrl("www.yourschoolwebsite.com/sports")
                                .imageUrl("https://images.unsplash.com/photo-1526676037777-05a232554f77?w=800&auto=format&fit=crop&q=80")
                                .bgColorHex("#10B981")
                                .status("Active")
                                .displayOrder(3)
                                .build()
                );
                bannerRepository.saveAll(initialBanners);
            }
        } catch (Exception e) {
            log.warn("Could not seed initial banners: {}", e.getMessage());
        }
    }

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

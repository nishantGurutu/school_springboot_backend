package com.school.management.repository;

import com.school.management.entity.CertificateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificateRepository extends JpaRepository<CertificateEntity, Long> {

    boolean existsByNameAndCertificateName(String name, String certificateName);
}

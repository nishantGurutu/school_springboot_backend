package com.school.management.repository;
import com.school.management.entity.PushDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PushDeviceRepository extends JpaRepository<PushDeviceEntity,String>{List<PushDeviceEntity> findByUserId(Long userId);}

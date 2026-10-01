package com.school.management.repository;

import com.school.management.entity.HolidayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<HolidayEntity, Long> {

    List<HolidayEntity> findAllByOrderByDateAsc();

    List<HolidayEntity> findByDate(String date);

    Optional<HolidayEntity> findFirstByDate(String date);

    @Query("SELECT h FROM HolidayEntity h WHERE h.date <= :date AND (h.endDate IS NULL OR h.endDate >= :date)")
    List<HolidayEntity> findHolidaysCoveringDate(@Param("date") String date);

    @Query("SELECT h FROM HolidayEntity h WHERE h.date LIKE :yearMonth%")
    List<HolidayEntity> findByYearMonth(@Param("yearMonth") String yearMonth);
}

package com.school.management.repository.transport;

import com.school.management.entity.transport.RouteStopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteStopRepository extends JpaRepository<RouteStopEntity, Long> {
    List<RouteStopEntity> findByRouteIdOrderByStopOrderAsc(Long routeId);
    List<RouteStopEntity> findBySchoolId(String schoolId);
    void deleteByRouteId(Long routeId);
}

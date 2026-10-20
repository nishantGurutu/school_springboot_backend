package com.school.management.service;

import com.school.management.dto.StaffRequest;
import com.school.management.dto.transport.BusAssignmentDto;
import com.school.management.dto.transport.TripDtos;
import com.school.management.entity.transport.BusEntity;
import com.school.management.entity.transport.TransportRouteEntity;
import com.school.management.repository.transport.*;
import com.school.management.service.transport.TransportService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TransportDriverFlowTest {
    @Autowired StaffService staff;
    @Autowired TeacherService teachers;
    @Autowired DriverBusAllocationService allocations;
    @Autowired TransportService transport;
    @Autowired BusRepository buses;
    @Autowired TransportRouteRepository routes;
    @Autowired DriverProfileRepository drivers;
    @Autowired BusLocationHistoryRepository history;
    @Autowired EntityManager entityManager;

    @Test void staffBusAssignmentTripCompletionAndMapUseTheSamePersistedBus() {
        var bus = buses.saveAndFlush(BusEntity.builder().busNumber("Flow Bus").registrationNumber("FLOW-TRANSPORT-01").capacity(30).build());
        assertTrue(allocations.availableBuses("SCH-001").stream().anyMatch(b -> b.getId().equals(bus.getId())));
        var request = new StaffRequest();
        request.setName("Transport Flow Driver"); request.setRole("DRIVER");
        request.setEmail("transport-flow@test.local"); request.setPassword("Flow-test-password-123");
        request.setStatus("ACTIVE"); request.setBusId(bus.getId());
        staff.create(request);
        entityManager.flush(); entityManager.clear();
        assertFalse(allocations.availableBuses("SCH-001").stream().anyMatch(b -> b.getId().equals(bus.getId())));
        var driver = drivers.findByEmailIgnoreCase(request.getEmail()).orElseThrow();
        assertEquals(bus.getId(), transport.getDriverDuty(request.getEmail()).getBusId());
        var route = routes.saveAndFlush(TransportRouteEntity.builder().routeName("Flow Route").routeCode("FLOW-ROUTE").build());
        transport.createAssignment(BusAssignmentDto.builder().driverId(driver.getId()).busId(bus.getId()).routeId(route.getId()).shift("MORNING").build(), "SCH-001");
        var start = TripDtos.StartTripRequest.builder().busId(bus.getId()).routeId(route.getId())
                .startLatitude(28.6).startLongitude(77.0).preTripCheckPassed(true).build();
        var trip = transport.startTrip(request.getEmail(), start);
        assertEquals(trip.getTripId(), transport.startTrip(request.getEmail(), start).getTripId());
        assertEquals(trip.getTripId(), transport.getDriverDuty(request.getEmail()).getActiveTripId());
        var now = LocalDateTime.now(ZoneOffset.UTC);
        var points = List.of(
                TripDtos.GpsPointDto.builder().latitude(28.601).longitude(77.001).recordedAt(now).speedKmh(20.0).build(),
                TripDtos.GpsPointDto.builder().latitude(28.602).longitude(77.002).recordedAt(now.plusSeconds(1)).speedKmh(0.0).build());
        var end = TripDtos.EndTripRequest.builder().points(points).totalDistanceKm(0.3).build();
        assertEquals("COMPLETED", transport.endTrip(trip.getTripId(), request.getEmail(), end).getStatus());
        transport.endTrip(trip.getTripId(), request.getEmail(), end);
        entityManager.flush(); entityManager.clear();
        var playback = transport.getTripPlayback(trip.getTripId(), "SCH-001");
        assertEquals(3, playback.getPathPoints().size());
        assertEquals(28.602, playback.getPathPoints().get(2).getLatitude());
        assertEquals("Flow Bus", playback.getBusNumber());
        assertNotNull(playback.getEndTime());
        assertNull(transport.getDriverDuty(request.getEmail()).getActiveTripId());
        assertEquals(3, history.findByTripIdOrderByRecordedAtAsc(trip.getTripId()).size());
    }
    @Test void addNewStaffPageCreatesDriverWithSelectedBus() {
        var bus = buses.saveAndFlush(BusEntity.builder().busNumber("Staff Page Bus").registrationNumber("FLOW-STAFF-PAGE-01").capacity(30).build());
        var availableBus = transport.getAllBuses("SCH-001").stream().filter(b -> b.getId().equals(bus.getId())).findFirst().orElseThrow();
        assertEquals("AVAILABLE", availableBus.getAssignmentStatus());
        assertTrue(availableBus.getAvailableForAssignment());
        var request = new com.school.management.dto.TeacherRequest();
        request.setEmployeeId("STAFF-DRIVER-FLOW"); request.setFirstName("Page"); request.setLastName("Driver");
        request.setRole("DRIVER"); request.setUserType("DRIVER"); request.setType("Driver");
        request.setDepartment("Transport"); request.setDesignation("Driver");
        request.setEmail("staff-page-driver@test.local"); request.setPhone("9876543210");
        request.setPassword("Flow-test-password-123"); request.setJoiningDate(java.time.LocalDate.now());
        request.setBusId(bus.getId());
        var created = teachers.create(request);
        entityManager.flush(); entityManager.clear();
        assertNotNull(created.getId());
        assertEquals(bus.getId(), created.getBusId());
        assertEquals(bus.getId(), teachers.getById(created.getId()).getBusId());
        var assignedBus = transport.getAllBuses("SCH-001").stream().filter(b -> b.getId().equals(bus.getId())).findFirst().orElseThrow();
        assertEquals("ASSIGNED", assignedBus.getAssignmentStatus());
        assertFalse(assignedBus.getAvailableForAssignment());
        assertNotNull(assignedBus.getAssignedDriverId());
        assertEquals(bus.getId(), transport.getDriverDuty(request.getEmail()).getBusId());
        assertFalse(allocations.availableBuses("SCH-001").stream().anyMatch(b -> b.getId().equals(bus.getId())));
        request.setEmployeeId("STAFF-SECOND-DRIVER");
        request.setEmail("staff-second-driver@test.local");
        assertThrows(com.school.management.exceptions.BadRequestException.class, () -> teachers.create(request));

    }

}

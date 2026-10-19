package com.school.management.service;

import com.school.management.dto.transport.TripDtos;
import com.school.management.entity.transport.*;
import com.school.management.exceptions.UnauthorizedException;
import com.school.management.repository.*;
import com.school.management.repository.transport.*;
import com.school.management.service.transport.impl.TransportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TripLocationTrackingTest {
    @Mock BusRepository buses;
    @Mock DriverProfileRepository drivers;
    @Mock TransportRouteRepository routes;
    @Mock RouteStopRepository stops;
    @Mock BusAssignmentRepository assignments;
    @Mock StudentTransportAssignmentRepository students;
    @Mock TransportTripRepository trips;
    @Mock BusLocationHistoryRepository history;
    @Mock TripStopEventRepository events;
    @Mock TripIncidentRepository incidents;
    @Mock TransportAlertRepository alerts;
    @Mock TransportAuditLogRepository audits;
    @Mock UserRepository users;
    @Mock PasswordEncoder passwords;
    @Mock TeacherRepository teachers;
    @Mock TransportFeePaymentRepository payments;
    @Mock StudentRepository studentRepository;
    @InjectMocks TransportServiceImpl service;
    TransportTripEntity trip;

    @BeforeEach void setup() {
        trip = TransportTripEntity.builder().id(10L).driverId(2L).busId(3L).routeId(4L)
                .schoolId("SCH-001").status("IN_PROGRESS").totalDistanceKm(0.0).build();
        DriverProfileEntity driver = DriverProfileEntity.builder().id(2L).email("driver@school.com").build();
        when(drivers.findByEmailIgnoreCase("driver@school.com")).thenReturn(Optional.of(driver));
        when(trips.findById(10L)).thenReturn(Optional.of(trip));
        when(trips.findForCompletion(10L)).thenReturn(Optional.of(trip));
        when(trips.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    TripDtos.GpsPointDto point(int secondsAgo) {
        return TripDtos.GpsPointDto.builder().latitude(25.6).longitude(85.1).speedKmh(20.0)
                .accuracyMeters(5.0).recordedAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(secondsAgo)).build();
    }

    @Test void liveTelemetryNeverWritesHistoryOrTrip() {
        service.publishLiveLocation(10L, "driver@school.com", point(10));
        service.publishLiveLocation(10L, "driver@school.com", point(5));
        verify(history, never()).saveAll(any());
        verify(trips, never()).save(any());
        verify(buses, never()).save(any());
        verify(trips, times(1)).findById(10L);
    }

    @Test void completionSavesOneBatchAndRetryDoesNotDuplicate() {
        var request = TripDtos.EndTripRequest.builder().points(List.of(point(20), point(10)))
                .totalDistanceKm(2.0).build();
        assertEquals("COMPLETED", service.endTrip(10L, "driver@school.com", request).getStatus());
        assertEquals("COMPLETED", service.endTrip(10L, "driver@school.com", request).getStatus());
        verify(history, times(1)).saveAll(argThat(batch -> ((List<?>) batch).size() == 2));
        verify(trips, times(1)).save(any());
    }

    @Test void wrongDriverCannotCompleteOrPublish() {
        when(drivers.findByEmailIgnoreCase("other@school.com")).thenReturn(Optional.of(
                DriverProfileEntity.builder().id(99L).build()));
        assertThrows(UnauthorizedException.class, () -> service.endTrip(10L, "other@school.com", new TripDtos.EndTripRequest()));
        assertThrows(UnauthorizedException.class, () -> service.publishLiveLocation(10L, "other@school.com", point(0)));
        verify(history, never()).saveAll(any());
    }

    @Test void studentAndInvalidPointsAreRejectedBeforeAnyHistorySave() {
        assertThrows(UnauthorizedException.class, () -> service.publishLiveLocation(10L, "student@school.com", point(0)));
        var invalid = point(0); invalid.setLatitude(Double.NaN);
        assertThrows(IllegalArgumentException.class, () -> service.endTrip(10L, "driver@school.com",
                TripDtos.EndTripRequest.builder().points(List.of(invalid)).build()));
        verify(history, never()).saveAll(any());
        assertEquals("IN_PROGRESS", trip.getStatus());
    }

    @Test void driverCannotStartAnotherDriversAssignment() {
        when(assignments.findById(44L)).thenReturn(Optional.of(BusAssignmentEntity.builder()
                .id(44L).driverId(99L).busId(3L).routeId(4L).status("ACTIVE").build()));
        assertThrows(UnauthorizedException.class, () -> service.startTrip("driver@school.com",
                TripDtos.StartTripRequest.builder().assignmentId(44L).build()));
        verify(trips, never()).save(any());
    }

    @Test void legacyPeriodicHistoryUploadIsDisabled() {
        assertThrows(IllegalStateException.class, () -> service.ingestGpsBatch(10L, "driver@school.com", new TripDtos.BatchGpsRequest()));
        verifyNoInteractions(history);
    }
    @Test void startCannotChangeAssignedBusOrRoute() {
        when(drivers.findByEmailIgnoreCase("driver@school.com")).thenReturn(Optional.of(
                DriverProfileEntity.builder().id(2L).schoolId("SCH-001").assignedBusId(3L).build()));
        when(assignments.findFirstByDriverIdAndStatusOrderByIdDesc(2L, "ACTIVE")).thenReturn(Optional.of(
                BusAssignmentEntity.builder().id(44L).schoolId("SCH-001").driverId(2L).busId(3L).routeId(4L).status("ACTIVE").build()));
        assertThrows(UnauthorizedException.class, () -> service.startTrip("driver@school.com",
                TripDtos.StartTripRequest.builder().busId(99L).routeId(4L).build()));
        assertThrows(UnauthorizedException.class, () -> service.startTrip("driver@school.com",
                TripDtos.StartTripRequest.builder().busId(3L).routeId(99L).build()));
        verify(assignments, never()).save(any());
        verify(trips, never()).save(any());
    }

    @Test void startWithoutScheduledDutyNeverAutoAssignsABus() {
        assertThrows(IllegalStateException.class, () -> service.startTrip("driver@school.com",
                TripDtos.StartTripRequest.builder().busId(3L).routeId(4L).build()));
        verify(assignments, never()).save(any());
        verify(trips, never()).save(any());
    }

    @Test void repeatedStartReturnsExistingTripAfterLockingAssignedBus() {
        when(drivers.findByEmailIgnoreCase("driver@school.com")).thenReturn(Optional.of(
                DriverProfileEntity.builder().id(2L).schoolId("SCH-001").assignedBusId(3L).build()));
        when(assignments.findFirstByDriverIdAndStatusOrderByIdDesc(2L, "ACTIVE")).thenReturn(Optional.of(
                BusAssignmentEntity.builder().id(44L).schoolId("SCH-001").driverId(2L).busId(3L).routeId(4L).status("ACTIVE").build()));
        when(buses.findForDriverAssignment(3L, "SCH-001")).thenReturn(Optional.of(
                BusEntity.builder().id(3L).status("ACTIVE").build()));
        when(trips.findFirstByDriverIdAndStatus(2L, "IN_PROGRESS")).thenReturn(Optional.of(trip));
        assertEquals(10L, service.startTrip("driver@school.com", new TripDtos.StartTripRequest()).getTripId());
        verify(buses).findForDriverAssignment(3L, "SCH-001");
        verify(trips, never()).save(any());
    }

}

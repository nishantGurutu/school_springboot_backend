package com.school.management.service;

import com.school.management.domain.user.Role;
import com.school.management.dto.transport.*;
import com.school.management.entity.*;
import com.school.management.entity.transport.*;
import com.school.management.repository.*;
import com.school.management.repository.transport.*;
import com.school.management.service.transport.impl.TransportServiceImpl;
import com.school.management.exceptions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentTransportFlowTest {
 @Mock BusRepository buses;
 @Mock DriverProfileRepository drivers;
 @Mock TransportRouteRepository routes;
 @Mock RouteStopRepository stops;
 @Mock BusAssignmentRepository duties;
 @Mock StudentTransportAssignmentRepository assignments;
 @Mock TransportTripRepository trips;
 @Mock BusLocationHistoryRepository history;
 @Mock TripStopEventRepository events;
 @Mock TripIncidentRepository incidents;
 @Mock TransportAlertRepository alerts;
 @Mock TransportAuditLogRepository audits;
 @Mock UserRepository users;
 @Mock PasswordEncoder passwords;
 @Mock TeacherRepository teachers;
 @Mock StaffRepository staff;
 @Mock TransportFeePaymentRepository payments;
 @Mock StudentRepository students;
 @Mock SafetyChecklistItemRepository checklist;
 @Mock DriverBusAllocationService allocations;
 @Mock PortalAccessService access;
 @InjectMocks TransportServiceImpl service;
 final StudentEntity student = StudentEntity.builder().id(42L).admissionNo("ADM42").name("Rudra").className("7").section("A").build();
 final LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

 void login() {
  when(access.actor()).thenReturn(UserEntity.builder().role(Role.STUDENT).build());
  when(access.children()).thenReturn(List.of(student));
 }
 StudentTransportAssignmentEntity assigned() {
  return StudentTransportAssignmentEntity.builder().id(5L).studentId(42L).admissionNo("ADM42").studentName("Rudra")
   .routeId(9L).pickupStopId(10L).dropStopId(11L).startDate(today.minusDays(1)).build();
 }
 void withAssignment(StudentTransportAssignmentEntity a) {
  when(assignments.findByStudentIdOrAdmissionNoIgnoreCaseOrderByIdDesc(42L,"ADM42")).thenReturn(List.of(a));
 }
 @Test void noAssignmentMeansNoTransportAndNoBusLookup() {
  login();
  assertFalse(service.getStudentTransport("ignored").isAssigned());
  verifyNoInteractions(buses, drivers, routes, trips);
 }
 @Test void inactiveExpiredAndFutureAssignmentsAreNotTransport() {
  login(); var a=assigned(); withAssignment(a);
  a.setStatus("INACTIVE"); assertFalse(service.getStudentTransport("ignored").isAssigned());
  a.setStatus("ACTIVE"); a.setEndDate(today.minusDays(1)); assertFalse(service.getStudentTransport("ignored").isAssigned());
  a.setEndDate(null); a.setStartDate(today.plusDays(1)); assertFalse(service.getStudentTransport("ignored").isAssigned());
 }
 @Test void otherStudentsSameNameOrMismatchedAdmissionNeverLeaks() {
  login(); var a=assigned(); a.setAdmissionNo("OTHER"); withAssignment(a);
  assertFalse(service.getStudentTransport("other@example.com").isAssigned());
  assertThrows(ResponseStatusException.class, () -> service.getParentChildTransport("ignored",99L));
 }
 @Test void legacyRandomIdCanResolveByCanonicalAdmission() {
  login(); var a=assigned(); a.setStudentId(9999L); withAssignment(a);
  var data=service.getStudentTransport("ignored");
  assertTrue(data.isAssigned()); assertEquals(42L,data.getStudentId());
  assertNull(data.getBusNumber()); assertNull(data.getDriverName()); assertEquals("SCHEDULED",data.getTripStatus());
  assertNull(data.getEstimatedArrivalMinutes()); assertFalse(data.isGpsFresh());
 }
 @Test void assignedStudentGetsRealBusDriverStopsAndFreshTelemetryOnly() {
  login(); withAssignment(assigned());
  when(routes.findById(9L)).thenReturn(Optional.of(TransportRouteEntity.builder().id(9L).routeName("North route").build()));
  when(stops.findById(10L)).thenReturn(Optional.of(RouteStopEntity.builder().id(10L).routeId(9L).stopName("Park").latitude(28.6).longitude(77.1).plannedMorningPickupTime("07:20 AM").build()));
  when(stops.findById(11L)).thenReturn(Optional.of(RouteStopEntity.builder().id(11L).routeId(9L).stopName("Gate").build()));
  var trip=TransportTripEntity.builder().id(12L).routeId(9L).busId(20L).driverId(30L).status("IN_PROGRESS")
    .lastLatitude(28.7).lastLongitude(77.2).lastLocationTime(LocalDateTime.now()).nextStopId(10L).nextStopEtaMinutes(4).build();
  when(trips.findFirstByRouteIdAndSchoolIdAndStatus(9L,"SCH-001","IN_PROGRESS")).thenReturn(Optional.of(trip));
  when(buses.findById(20L)).thenReturn(Optional.of(BusEntity.builder().id(20L).busNumber("BUS20").build()));
  when(drivers.findById(30L)).thenReturn(Optional.of(DriverProfileEntity.builder().id(30L).name("Driver 30").phone("9999999999").build()));
  var data=service.getStudentTransport("ignored");
  assertTrue(data.isAssigned()); assertEquals("BUS20",data.getBusNumber()); assertEquals("Driver 30",data.getDriverName());
  assertEquals("Park",data.getPickupStopName()); assertEquals("07:20 AM",data.getPickupTime());
  assertEquals(28.7,data.getBusLatitude()); assertTrue(data.isGpsFresh()); assertEquals(4,data.getEstimatedArrivalMinutes());
  trip.setLastLocationTime(LocalDateTime.now().minusMinutes(2));
  data=service.getStudentTransport("ignored"); assertFalse(data.isGpsFresh()); assertNull(data.getBusLatitude()); assertNull(data.getEstimatedArrivalMinutes());
 }
 @Test void creatingAssignmentUsesRealStudentIdAndRejectsWrongRouteStop() {
  when(students.findByAdmissionNoIgnoreCase("ADM42")).thenReturn(Optional.of(student));
  when(routes.findByIdAndSchoolId(9L,"SCH-001")).thenReturn(Optional.of(TransportRouteEntity.builder().id(9L).build()));
  when(assignments.save(any())).thenAnswer(i -> i.getArgument(0));
  var request=StudentAssignmentDto.builder().studentId(9999L).studentName("Wrong name").admissionNo("ADM42").routeId(9L).monthlyFee(1200.0).build();
  var result=service.assignStudentToRoute(request,"SCH-001");
  assertEquals(42L,result.getStudentId()); assertEquals("Rudra",result.getStudentName());
  request.setPickupStopId(999L);
  assertThrows(BadRequestException.class,()->service.assignStudentToRoute(request,"SCH-001"));
 }
}

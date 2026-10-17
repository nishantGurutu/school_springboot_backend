package com.school.management.service.transport.impl;

import com.school.management.domain.user.Role;
import com.school.management.dto.transport.*;
import com.school.management.entity.TeacherEntity;
import com.school.management.entity.UserEntity;
import com.school.management.entity.transport.*;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.exceptions.UnauthorizedException;
import com.school.management.repository.TeacherRepository;
import com.school.management.repository.UserRepository;
import com.school.management.repository.StudentRepository;
import com.school.management.repository.transport.*;
import com.school.management.service.transport.TransportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransportServiceImpl implements TransportService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TransportServiceImpl.class);

    private final BusRepository busRepository;
    private final DriverProfileRepository driverRepository;
    private final TransportRouteRepository routeRepository;
    private final RouteStopRepository stopRepository;
    private final BusAssignmentRepository assignmentRepository;
    private final StudentTransportAssignmentRepository studentAssignmentRepository;
    private final TransportTripRepository tripRepository;
    private final BusLocationHistoryRepository locationHistoryRepository;
    private final TripStopEventRepository stopEventRepository;
    private final TripIncidentRepository incidentRepository;
    private final TransportAlertRepository alertRepository;
    private final TransportAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TeacherRepository teacherRepository;
    private final TransportFeePaymentRepository feePaymentRepository;
    private final StudentRepository studentRepository;

    public TransportServiceImpl(
            BusRepository busRepository,
            DriverProfileRepository driverRepository,
            TransportRouteRepository routeRepository,
            RouteStopRepository stopRepository,
            BusAssignmentRepository assignmentRepository,
            StudentTransportAssignmentRepository studentAssignmentRepository,
            TransportTripRepository tripRepository,
            BusLocationHistoryRepository locationHistoryRepository,
            TripStopEventRepository stopEventRepository,
            TripIncidentRepository incidentRepository,
            TransportAlertRepository alertRepository,
            TransportAuditLogRepository auditLogRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TeacherRepository teacherRepository,
            TransportFeePaymentRepository feePaymentRepository,
            StudentRepository studentRepository
    ) {
        this.busRepository = busRepository;
        this.driverRepository = driverRepository;
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
        this.assignmentRepository = assignmentRepository;
        this.studentAssignmentRepository = studentAssignmentRepository;
        this.tripRepository = tripRepository;
        this.locationHistoryRepository = locationHistoryRepository;
        this.stopEventRepository = stopEventRepository;
        this.incidentRepository = incidentRepository;
        this.alertRepository = alertRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.teacherRepository = teacherRepository;
        this.feePaymentRepository = feePaymentRepository;
        this.studentRepository = studentRepository;
    }

    private static final String DEFAULT_SCHOOL_ID = "SCH-001";

    private String resolveSchool(String schoolId) {
        return (schoolId != null && !schoolId.isBlank()) ? schoolId : DEFAULT_SCHOOL_ID;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartupSeedTransportData() {
        try {
            seedInitialTransportDataIfEmpty(DEFAULT_SCHOOL_ID);
        } catch (Exception e) {
            log.warn("Notice seeding initial transport data on startup: {}", e.getMessage());
        }
    }

    // ==========================================
    // OVERVIEW & DASHBOARD
    // ==========================================

    @Override
    public TransportOverviewDto getOverview(String schoolId) {
        String sId = resolveSchool(schoolId);
        seedInitialTransportDataIfEmpty(sId);

        long totalBuses = busRepository.countBySchoolId(sId);
        long activeTrips = tripRepository.countBySchoolIdAndStatus(sId, "IN_PROGRESS");
        long idleBuses = busRepository.countBySchoolIdAndStatus(sId, "ACTIVE") - activeTrips;
        if (idleBuses < 0) idleBuses = 0;

        long openAlerts = alertRepository.countBySchoolIdAndStatus(sId, "OPEN");
        long totalRoutes = routeRepository.countBySchoolId(sId);
        long totalDrivers = driverRepository.countBySchoolId(sId);
        long totalStudents = studentAssignmentRepository.count();

        List<TripDtos.LiveTrackingBusDto> liveBuses = getLiveTracking(sId);
        long offlineGps = liveBuses.stream().filter(b -> Boolean.FALSE.equals(b.getIsGpsOnline())).count();
        long delayedTrips = alertRepository.findBySchoolIdAndStatusOrderByCreatedAtDesc(sId, "OPEN")
                .stream().filter(a -> "DELAY".equalsIgnoreCase(a.getAlertType())).count();

        List<TransportAlertDto> recentAlerts = getAlerts(sId, null).stream().limit(5).collect(Collectors.toList());

        return TransportOverviewDto.builder()
                .totalBuses(totalBuses)
                .activeTrips(activeTrips)
                .idleBuses(idleBuses)
                .delayedTrips(delayedTrips)
                .offlineGpsCount(offlineGps)
                .openAlertsCount(openAlerts)
                .totalRoutes(totalRoutes)
                .totalDrivers(totalDrivers)
                .totalStudentsAssigned(totalStudents)
                .liveBuses(liveBuses)
                .recentAlerts(recentAlerts)
                .build();
    }

    // ==========================================
    // BUS MANAGEMENT
    // ==========================================

    @Override
    public List<BusDto> getAllBuses(String schoolId) {
        String sId = resolveSchool(schoolId);
        seedInitialTransportDataIfEmpty(sId);
        return busRepository.findBySchoolId(sId).stream()
                .map(this::mapToBusDto)
                .collect(Collectors.toList());
    }

    @Override
    public BusDto getBusById(Long id, String schoolId) {
        String sId = resolveSchool(schoolId);
        BusEntity bus = busRepository.findByIdAndSchoolId(id, sId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + id));
        return mapToBusDto(bus);
    }

    @Override
    public BusDto createBus(BusDto dto, String schoolId) {
        String sId = resolveSchool(schoolId);
        BusEntity entity = BusEntity.builder()
                .schoolId(sId)
                .busNumber(dto.getBusNumber())
                .registrationNumber(dto.getRegistrationNumber().trim().toUpperCase())
                .capacity(dto.getCapacity() != null ? dto.getCapacity() : 40)
                .model(dto.getModel())
                .manufacturer(dto.getManufacturer())
                .fuelType(dto.getFuelType() != null ? dto.getFuelType() : "DIESEL")
                .gpsDeviceId(dto.getGpsDeviceId())
                .insuranceExpiryDate(dto.getInsuranceExpiryDate())
                .fitnessExpiryDate(dto.getFitnessExpiryDate())
                .pollutionExpiryDate(dto.getPollutionExpiryDate())
                .permitExpiryDate(dto.getPermitExpiryDate())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .build();

        BusEntity saved = busRepository.save(entity);
        logAudit(sId, "BUS_CREATED", "BUS", saved.getId(), "Created bus: " + saved.getBusNumber());
        return mapToBusDto(saved);
    }

    @Override
    public BusDto updateBus(Long id, BusDto dto, String schoolId) {
        String sId = resolveSchool(schoolId);
        BusEntity bus = busRepository.findByIdAndSchoolId(id, sId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + id));

        if (dto.getBusNumber() != null) bus.setBusNumber(dto.getBusNumber());
        if (dto.getRegistrationNumber() != null) bus.setRegistrationNumber(dto.getRegistrationNumber().trim().toUpperCase());
        if (dto.getCapacity() != null) bus.setCapacity(dto.getCapacity());
        if (dto.getModel() != null) bus.setModel(dto.getModel());
        if (dto.getManufacturer() != null) bus.setManufacturer(dto.getManufacturer());
        if (dto.getFuelType() != null) bus.setFuelType(dto.getFuelType());
        if (dto.getGpsDeviceId() != null) bus.setGpsDeviceId(dto.getGpsDeviceId());
        if (dto.getInsuranceExpiryDate() != null) bus.setInsuranceExpiryDate(dto.getInsuranceExpiryDate());
        if (dto.getFitnessExpiryDate() != null) bus.setFitnessExpiryDate(dto.getFitnessExpiryDate());
        if (dto.getPollutionExpiryDate() != null) bus.setPollutionExpiryDate(dto.getPollutionExpiryDate());
        if (dto.getPermitExpiryDate() != null) bus.setPermitExpiryDate(dto.getPermitExpiryDate());
        if (dto.getStatus() != null) bus.setStatus(dto.getStatus());

        BusEntity updated = busRepository.save(bus);
        logAudit(sId, "BUS_UPDATED", "BUS", updated.getId(), "Updated bus: " + updated.getBusNumber());
        return mapToBusDto(updated);
    }

    @Override
    public void deleteBus(Long id, String schoolId) {
        String sId = resolveSchool(schoolId);
        BusEntity bus = busRepository.findByIdAndSchoolId(id, sId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + id));
        busRepository.delete(bus);
        logAudit(sId, "BUS_DELETED", "BUS", id, "Deleted bus: " + bus.getBusNumber());
    }

    private BusDto mapToBusDto(BusEntity bus) {
        BusDto dto = BusDto.builder()
                .id(bus.getId())
                .schoolId(bus.getSchoolId())
                .busNumber(bus.getBusNumber())
                .registrationNumber(bus.getRegistrationNumber())
                .capacity(bus.getCapacity())
                .model(bus.getModel())
                .manufacturer(bus.getManufacturer())
                .fuelType(bus.getFuelType())
                .gpsDeviceId(bus.getGpsDeviceId())
                .insuranceExpiryDate(bus.getInsuranceExpiryDate())
                .fitnessExpiryDate(bus.getFitnessExpiryDate())
                .pollutionExpiryDate(bus.getPollutionExpiryDate())
                .permitExpiryDate(bus.getPermitExpiryDate())
                .status(bus.getStatus())
                .currentSpeed(bus.getCurrentSpeed())
                .lastLatitude(bus.getLastLatitude())
                .lastLongitude(bus.getLastLongitude())
                .lastGpsUpdate(bus.getLastGpsUpdate())
                .build();

        assignmentRepository.findByBusIdAndStatus(bus.getId(), "ACTIVE").stream().findFirst().ifPresent(as -> {
            dto.setAssignedDriverId(as.getDriverId());
            driverRepository.findById(as.getDriverId()).ifPresent(d -> dto.setAssignedDriverName(d.getName()));
            dto.setAssignedRouteId(as.getRouteId());
            routeRepository.findById(as.getRouteId()).ifPresent(r -> dto.setAssignedRouteName(r.getRouteName()));
        });

        tripRepository.findFirstByBusIdAndStatus(bus.getId(), "IN_PROGRESS").ifPresent(t -> {
            dto.setActiveTripStatus("IN_PROGRESS");
            dto.setActiveTripId(t.getId());
        });

        return dto;
    }

    // ==========================================
    // DRIVER MANAGEMENT
    // ==========================================

    @Override
    public List<DriverDto> getAllDrivers(String schoolId) {
        String sId = resolveSchool(schoolId);
        seedInitialTransportDataIfEmpty(sId);
        syncDriversFromStaff(sId);
        return driverRepository.findBySchoolId(sId).stream()
                .map(this::mapToDriverDto)
                .collect(Collectors.toList());
    }

    private void syncDriversFromStaff(String sId) {
        try {
            List<TeacherEntity> staffList = teacherRepository.findAll();
            for (TeacherEntity staff : staffList) {
                boolean isDriver = (staff.getDesignation() != null && staff.getDesignation().toLowerCase().contains("driver"))
                        || (staff.getDepartment() != null && staff.getDepartment().toLowerCase().contains("transport"))
                        || ("Driver".equalsIgnoreCase(staff.getType()));
                if (isDriver) {
                    String email = (staff.getEmail() != null && !staff.getEmail().isBlank())
                            ? staff.getEmail().trim().toLowerCase()
                            : ("driver." + staff.getEmployeeId() + "@school.com").toLowerCase();

                    if (driverRepository.findByEmailIgnoreCase(email).isEmpty()) {
                        String fullName = ((staff.getFirstName() != null ? staff.getFirstName() : "") + " " +
                                (staff.getLastName() != null ? staff.getLastName() : "")).trim();
                        if (fullName.isBlank()) fullName = "Staff Driver " + staff.getEmployeeId();

                        DriverProfileEntity newDriver = DriverProfileEntity.builder()
                                .schoolId(sId)
                                .name(fullName)
                                .email(email)
                                .phone(staff.getPhone() != null ? staff.getPhone() : "+91 98000 00000")
                                .licenseNumber("DL-" + (staff.getEmployeeId() != null ? staff.getEmployeeId() : "IND" + (1000 + staff.getId())))
                                .licenseExpiryDate(LocalDate.now().plusYears(4))
                                .licenseCategory("COMMERCIAL_HEAVY")
                                .experienceYears(staff.getExperienceYears() != null ? staff.getExperienceYears() : 5)
                                .joiningDate(staff.getJoiningDate() != null ? staff.getJoiningDate() : LocalDate.now())
                                .verificationStatus("VERIFIED")
                                .status("ACTIVE")
                                .avatarUrl(staff.getAvatar())
                                .emergencyContact(staff.getPhone())
                                .address(staff.getAddress() != null ? staff.getAddress() : "Campus Staff Quarters")
                                .build();
                        driverRepository.save(newDriver);
                        log.info("Auto-synced teacher/staff driver {} into transport driver profiles.", fullName);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Auto-sync drivers from staff failed: {}", ex.getMessage());
        }
    }

    @Override
    public DriverDto getDriverById(Long id, String schoolId) {
        String sId = resolveSchool(schoolId);
        DriverProfileEntity driver = driverRepository.findByIdAndSchoolId(id, sId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
        return mapToDriverDto(driver);
    }

    @Override
    public DriverDailyDetailsDto getDriverDailyDetails(Long id, String schoolId) {
        String sId = resolveSchool(schoolId);
        seedInitialTransportDataIfEmpty(sId);
        DriverProfileEntity driver = driverRepository.findByIdAndSchoolId(id, sId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));

        DriverDto driverDto = mapToDriverDto(driver);

        List<TransportTripEntity> allTrips = tripRepository.findBySchoolId(sId).stream()
                .filter(t -> t.getDriverId().equals(driver.getId()))
                .sorted(Comparator.comparing(TransportTripEntity::getId).reversed())
                .collect(Collectors.toList());

        TransportTripEntity todayTripEntity = allTrips.stream()
                .filter(t -> LocalDate.now().equals(t.getTripDate()))
                .findFirst()
                .orElse(null);

        TripDtos.TripSummaryDto todayTripDto = todayTripEntity != null ? mapToTripSummaryDto(todayTripEntity) : null;
        List<TripDtos.TripSummaryDto> recentTrips = allTrips.stream()
                .limit(15)
                .map(this::mapToTripSummaryDto)
                .collect(Collectors.toList());

        boolean checklistCompleted = todayTripDto != null && Boolean.TRUE.equals(todayTripDto.getPreTripCheckPassed());
        String checklistSummary = checklistCompleted
                ? "5/5 Verified • Pre-departure Inspection Complete"
                : (todayTripDto != null ? "Checklist Pending / Incomplete" : "Not submitted today");

        String startLoc = todayTripDto != null && todayTripDto.getStartLocationName() != null
                ? todayTripDto.getStartLocationName()
                : (todayTripDto != null && todayTripDto.getStartLatitude() != null
                    ? String.format("%.4f, %.4f", todayTripDto.getStartLatitude(), todayTripDto.getStartLongitude())
                    : "Not departed yet");

        String startTime = todayTripDto != null && todayTripDto.getActualStartTime() != null
                ? todayTripDto.getActualStartTime().toString()
                : "Not started yet";

        return DriverDailyDetailsDto.builder()
                .driver(driverDto)
                .todayTrip(todayTripDto)
                .recentTrips(recentTrips)
                .todayChecklistCompleted(checklistCompleted)
                .todayChecklistSummary(checklistSummary)
                .todayStartLocation(startLoc)
                .todayStartTime(startTime)
                .build();
    }

    @Override
    public DriverDto createDriver(DriverDto dto, String schoolId) {
        String sId = resolveSchool(schoolId);
        String email = dto.getEmail().trim().toLowerCase();

        // Create or find user account with Role.DRIVER
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            String rawPassword = (dto.getPassword() != null && !dto.getPassword().isBlank()) ? dto.getPassword() : "password";
            UserEntity newUser = UserEntity.builder()
                    .email(email)
                    .name(dto.getName())
                    .password(passwordEncoder.encode(rawPassword))
                    .role(Role.DRIVER)
                    .enabled(true)
                    .build();
            return userRepository.save(newUser);
        });

        DriverProfileEntity driver = DriverProfileEntity.builder()
                .schoolId(sId)
                .userId(user.getId())
                .name(dto.getName())
                .email(email)
                .phone(dto.getPhone())
                .licenseNumber(dto.getLicenseNumber())
                .licenseExpiryDate(dto.getLicenseExpiryDate())
                .licenseCategory(dto.getLicenseCategory() != null ? dto.getLicenseCategory() : "COMMERCIAL_HEAVY")
                .experienceYears(dto.getExperienceYears() != null ? dto.getExperienceYears() : 5)
                .joiningDate(dto.getJoiningDate() != null ? dto.getJoiningDate() : LocalDate.now())
                .verificationStatus(dto.getVerificationStatus() != null ? dto.getVerificationStatus() : "VERIFIED")
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .avatarUrl(dto.getAvatarUrl())
                .emergencyContact(dto.getEmergencyContact())
                .address(dto.getAddress())
                .build();

        DriverProfileEntity saved = driverRepository.save(driver);
        logAudit(sId, "DRIVER_REGISTERED", "DRIVER", saved.getId(), "Registered driver: " + saved.getName());
        return mapToDriverDto(saved);
    }

    @Override
    public DriverDto updateDriver(Long id, DriverDto dto, String schoolId) {
        String sId = resolveSchool(schoolId);
        DriverProfileEntity driver = driverRepository.findByIdAndSchoolId(id, sId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));

        if (dto.getName() != null) driver.setName(dto.getName());
        if (dto.getPhone() != null) driver.setPhone(dto.getPhone());
        if (dto.getLicenseNumber() != null) driver.setLicenseNumber(dto.getLicenseNumber());
        if (dto.getLicenseExpiryDate() != null) driver.setLicenseExpiryDate(dto.getLicenseExpiryDate());
        if (dto.getLicenseCategory() != null) driver.setLicenseCategory(dto.getLicenseCategory());
        if (dto.getExperienceYears() != null) driver.setExperienceYears(dto.getExperienceYears());
        if (dto.getJoiningDate() != null) driver.setJoiningDate(dto.getJoiningDate());
        if (dto.getVerificationStatus() != null) driver.setVerificationStatus(dto.getVerificationStatus());
        if (dto.getStatus() != null) driver.setStatus(dto.getStatus());
        if (dto.getAvatarUrl() != null) driver.setAvatarUrl(dto.getAvatarUrl());
        if (dto.getEmergencyContact() != null) driver.setEmergencyContact(dto.getEmergencyContact());
        if (dto.getAddress() != null) driver.setAddress(dto.getAddress());

        DriverProfileEntity updated = driverRepository.save(driver);
        logAudit(sId, "DRIVER_UPDATED", "DRIVER", updated.getId(), "Updated driver: " + updated.getName());
        return mapToDriverDto(updated);
    }

    @Override
    public void deleteDriver(Long id, String schoolId) {
        String sId = resolveSchool(schoolId);
        DriverProfileEntity driver = driverRepository.findByIdAndSchoolId(id, sId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
        driverRepository.delete(driver);
        logAudit(sId, "DRIVER_DELETED", "DRIVER", id, "Deleted driver: " + driver.getName());
    }

    private DriverDto mapToDriverDto(DriverProfileEntity driver) {
        DriverDto dto = DriverDto.builder()
                .id(driver.getId())
                .schoolId(driver.getSchoolId())
                .userId(driver.getUserId())
                .name(driver.getName())
                .email(driver.getEmail())
                .phone(driver.getPhone())
                .licenseNumber(driver.getLicenseNumber())
                .licenseExpiryDate(driver.getLicenseExpiryDate())
                .licenseCategory(driver.getLicenseCategory())
                .experienceYears(driver.getExperienceYears())
                .joiningDate(driver.getJoiningDate())
                .verificationStatus(driver.getVerificationStatus())
                .status(driver.getStatus())
                .avatarUrl(driver.getAvatarUrl())
                .emergencyContact(driver.getEmergencyContact())
                .address(driver.getAddress())
                .dutyStatus("OFF_DUTY")
                .build();

        assignmentRepository.findFirstByDriverIdAndStatusOrderByIdDesc(driver.getId(), "ACTIVE").ifPresent(as -> {
            dto.setAssignedBusId(as.getBusId());
            busRepository.findById(as.getBusId()).ifPresent(b -> dto.setAssignedBusNumber(b.getBusNumber()));
            dto.setAssignedRouteId(as.getRouteId());
            routeRepository.findById(as.getRouteId()).ifPresent(r -> dto.setAssignedRouteName(r.getRouteName()));
            dto.setDutyStatus("SCHEDULED");
        });

        // Check if there is an in-progress trip or today's latest trip
        tripRepository.findBySchoolId(driver.getSchoolId()).stream()
                .filter(t -> t.getDriverId().equals(driver.getId()))
                .filter(t -> LocalDate.now().equals(t.getTripDate()))
                .max(Comparator.comparing(TransportTripEntity::getId))
                .ifPresent(trip -> {
                    dto.setTodayTripId(trip.getId());
                    dto.setTodayTripStatus(trip.getStatus());
                    dto.setTodayTripStartTime(trip.getActualStartTime());
                    dto.setTodayStartLatitude(trip.getStartLatitude());
                    dto.setTodayStartLongitude(trip.getStartLongitude());
                    dto.setTodayStartLocationName(trip.getStartLocationName() != null ? trip.getStartLocationName()
                            : (trip.getStartLatitude() != null ? String.format("%.4f, %.4f", trip.getStartLatitude(), trip.getStartLongitude()) : null));
                    dto.setTodayPreTripCheckPassed(trip.getPreTripCheckPassed());
                    dto.setTodayPreTripNotes(trip.getPreTripNotes());
                    dto.setTodayChkBrakes(trip.getChkBrakes());
                    dto.setTodayChkLights(trip.getChkLights());
                    dto.setTodayChkTires(trip.getChkTires());
                    dto.setTodayChkEmergencyKit(trip.getChkEmergencyKit());
                    dto.setTodayChkGpsActive(trip.getChkGpsActive());
                    if ("IN_PROGRESS".equals(trip.getStatus())) {
                        dto.setDutyStatus("IN_TRIP");
                    }
                });

        return dto;
    }

    // ==========================================
    // ROUTE & STOPS MANAGEMENT
    // ==========================================

    @Override
    public List<RouteDto> getAllRoutes(String schoolId) {
        String sId = resolveSchool(schoolId);
        seedInitialTransportDataIfEmpty(sId);
        return routeRepository.findBySchoolId(sId).stream()
                .map(this::mapToRouteDto)
                .collect(Collectors.toList());
    }

    @Override
    public RouteDto getRouteById(Long id, String schoolId) {
        String sId = resolveSchool(schoolId);
        TransportRouteEntity route = routeRepository.findByIdAndSchoolId(id, sId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + id));
        return mapToRouteDto(route);
    }

    @Override
    public RouteDto createRoute(RouteDto dto, String schoolId) {
        String sId = resolveSchool(schoolId);
        TransportRouteEntity route = TransportRouteEntity.builder()
                .schoolId(sId)
                .routeName(dto.getRouteName())
                .routeCode(dto.getRouteCode().trim().toUpperCase())
                .description(dto.getDescription())
                .startLocation(dto.getStartLocation())
                .endLocation(dto.getEndLocation())
                .areaLocality(dto.getAreaLocality())
                .fare(dto.getFare() != null ? dto.getFare() : 1500.0)
                .plannedDurationMinutes(dto.getPlannedDurationMinutes() != null ? dto.getPlannedDurationMinutes() : 45)
                .totalDistanceKm(dto.getTotalDistanceKm() != null ? dto.getTotalDistanceKm() : 15.0)
                .morningStartTime(dto.getMorningStartTime() != null ? dto.getMorningStartTime() : "07:15 AM")
                .eveningStartTime(dto.getEveningStartTime() != null ? dto.getEveningStartTime() : "02:45 PM")
                .speedLimitKmh(dto.getSpeedLimitKmh() != null ? dto.getSpeedLimitKmh() : 50)
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .build();

        TransportRouteEntity saved = routeRepository.save(route);

        if (dto.getStops() != null && !dto.getStops().isEmpty()) {
            saveRouteStops(saved.getId(), dto.getStops(), sId);
        }

        logAudit(sId, "ROUTE_CREATED", "ROUTE", saved.getId(), "Created route: " + saved.getRouteName());
        return mapToRouteDto(saved);
    }

    @Override
    public RouteDto updateRoute(Long id, RouteDto dto, String schoolId) {
        String sId = resolveSchool(schoolId);
        TransportRouteEntity route = routeRepository.findByIdAndSchoolId(id, sId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + id));

        if (dto.getRouteName() != null) route.setRouteName(dto.getRouteName());
        if (dto.getRouteCode() != null) route.setRouteCode(dto.getRouteCode().trim().toUpperCase());
        if (dto.getDescription() != null) route.setDescription(dto.getDescription());
        if (dto.getStartLocation() != null) route.setStartLocation(dto.getStartLocation());
        if (dto.getEndLocation() != null) route.setEndLocation(dto.getEndLocation());
        if (dto.getAreaLocality() != null) route.setAreaLocality(dto.getAreaLocality());
        if (dto.getFare() != null) route.setFare(dto.getFare());
        if (dto.getPlannedDurationMinutes() != null) route.setPlannedDurationMinutes(dto.getPlannedDurationMinutes());
        if (dto.getTotalDistanceKm() != null) route.setTotalDistanceKm(dto.getTotalDistanceKm());
        if (dto.getMorningStartTime() != null) route.setMorningStartTime(dto.getMorningStartTime());
        if (dto.getEveningStartTime() != null) route.setEveningStartTime(dto.getEveningStartTime());
        if (dto.getSpeedLimitKmh() != null) route.setSpeedLimitKmh(dto.getSpeedLimitKmh());
        if (dto.getStatus() != null) route.setStatus(dto.getStatus());

        TransportRouteEntity updated = routeRepository.save(route);

        if (dto.getStops() != null) {
            saveRouteStops(updated.getId(), dto.getStops(), sId);
        }

        logAudit(sId, "ROUTE_UPDATED", "ROUTE", updated.getId(), "Updated route: " + updated.getRouteName());
        return mapToRouteDto(updated);
    }

    @Override
    public void deleteRoute(Long id, String schoolId) {
        String sId = resolveSchool(schoolId);
        TransportRouteEntity route = routeRepository.findByIdAndSchoolId(id, sId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + id));
        stopRepository.deleteByRouteId(id);
        routeRepository.delete(route);
        logAudit(sId, "ROUTE_DELETED", "ROUTE", id, "Deleted route: " + route.getRouteName());
    }

    @Override
    public List<RouteStopDto> saveRouteStops(Long routeId, List<RouteStopDto> stops, String schoolId) {
        String sId = resolveSchool(schoolId);
        stopRepository.deleteByRouteId(routeId);

        List<RouteStopEntity> entities = new ArrayList<>();
        int order = 1;
        for (RouteStopDto stop : stops) {
            entities.add(RouteStopEntity.builder()
                    .schoolId(sId)
                    .routeId(routeId)
                    .stopName(stop.getStopName())
                    .stopOrder(order++)
                    .latitude(stop.getLatitude() != null ? stop.getLatitude() : 25.6000)
                    .longitude(stop.getLongitude() != null ? stop.getLongitude() : 85.1400)
                    .plannedMorningPickupTime(stop.getPlannedMorningPickupTime())
                    .plannedEveningDropTime(stop.getPlannedEveningDropTime())
                    .geofenceRadiusMeters(stop.getGeofenceRadiusMeters() != null ? stop.getGeofenceRadiusMeters() : 50)
                    .landmark(stop.getLandmark())
                    .assignedStudentsCount(stop.getAssignedStudentsCount() != null ? stop.getAssignedStudentsCount() : 0)
                    .build());
        }

        List<RouteStopEntity> saved = stopRepository.saveAll(entities);
        return saved.stream().map(this::mapToRouteStopDto).collect(Collectors.toList());
    }

    private RouteDto mapToRouteDto(TransportRouteEntity route) {
        List<RouteStopDto> stops = stopRepository.findByRouteIdOrderByStopOrderAsc(route.getId()).stream()
                .map(this::mapToRouteStopDto)
                .collect(Collectors.toList());

        long assignedStudents = studentAssignmentRepository.countByRouteId(route.getId());

        return RouteDto.builder()
                .id(route.getId())
                .schoolId(route.getSchoolId())
                .routeName(route.getRouteName())
                .routeCode(route.getRouteCode())
                .description(route.getDescription())
                .startLocation(route.getStartLocation())
                .endLocation(route.getEndLocation())
                .areaLocality(route.getAreaLocality())
                .fare(route.getFare())
                .plannedDurationMinutes(route.getPlannedDurationMinutes())
                .totalDistanceKm(route.getTotalDistanceKm())
                .morningStartTime(route.getMorningStartTime())
                .eveningStartTime(route.getEveningStartTime())
                .speedLimitKmh(route.getSpeedLimitKmh())
                .status(route.getStatus())
                .stopsCount(stops.size())
                .assignedStudentsCount((int) assignedStudents)
                .stops(stops)
                .build();
    }

    private RouteStopDto mapToRouteStopDto(RouteStopEntity stop) {
        return RouteStopDto.builder()
                .id(stop.getId())
                .routeId(stop.getRouteId())
                .stopName(stop.getStopName())
                .stopOrder(stop.getStopOrder())
                .latitude(stop.getLatitude())
                .longitude(stop.getLongitude())
                .plannedMorningPickupTime(stop.getPlannedMorningPickupTime())
                .plannedEveningDropTime(stop.getPlannedEveningDropTime())
                .geofenceRadiusMeters(stop.getGeofenceRadiusMeters())
                .landmark(stop.getLandmark())
                .assignedStudentsCount(stop.getAssignedStudentsCount())
                .build();
    }

    // ==========================================
    // BUS ASSIGNMENTS
    // ==========================================

    @Override
    public List<BusAssignmentDto> getAllAssignments(String schoolId) {
        String sId = resolveSchool(schoolId);
        seedInitialTransportDataIfEmpty(sId);
        return assignmentRepository.findBySchoolId(sId).stream()
                .map(this::mapToAssignmentDto)
                .collect(Collectors.toList());
    }

    @Override
    public BusAssignmentDto createAssignment(BusAssignmentDto dto, String schoolId) {
        String sId = resolveSchool(schoolId);

        // Conflict check: driver or bus active on overlapping period
        List<BusAssignmentEntity> existingDriverAssignments = assignmentRepository.findByDriverIdAndStatus(dto.getDriverId(), "ACTIVE");
        if (!existingDriverAssignments.isEmpty()) {
            existingDriverAssignments.forEach(as -> {
                as.setStatus("INACTIVE");
                assignmentRepository.save(as);
            });
        }

        BusAssignmentEntity entity = BusAssignmentEntity.builder()
                .schoolId(sId)
                .driverId(dto.getDriverId())
                .busId(dto.getBusId())
                .routeId(dto.getRouteId())
                .shift(dto.getShift() != null ? dto.getShift() : "BOTH")
                .effectiveStartDate(dto.getEffectiveStartDate() != null ? dto.getEffectiveStartDate() : LocalDate.now())
                .effectiveEndDate(dto.getEffectiveEndDate())
                .status("ACTIVE")
                .notes(dto.getNotes())
                .build();

        BusAssignmentEntity saved = assignmentRepository.save(entity);

        // Update driver record
        driverRepository.findById(dto.getDriverId()).ifPresent(d -> {
            d.setAssignedBusId(dto.getBusId());
            d.setAssignedRouteId(dto.getRouteId());
            driverRepository.save(d);
        });

        logAudit(sId, "ASSIGNMENT_CREATED", "ASSIGNMENT", saved.getId(), "Assigned driver " + dto.getDriverId() + " to bus " + dto.getBusId());
        return mapToAssignmentDto(saved);
    }

    @Override
    public BusAssignmentDto updateAssignment(Long id, BusAssignmentDto dto, String schoolId) {
        String sId = resolveSchool(schoolId);
        BusAssignmentEntity as = assignmentRepository.findByIdAndSchoolId(id, sId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + id));

        if (dto.getDriverId() != null) as.setDriverId(dto.getDriverId());
        if (dto.getBusId() != null) as.setBusId(dto.getBusId());
        if (dto.getRouteId() != null) as.setRouteId(dto.getRouteId());
        if (dto.getShift() != null) as.setShift(dto.getShift());
        if (dto.getEffectiveStartDate() != null) as.setEffectiveStartDate(dto.getEffectiveStartDate());
        if (dto.getEffectiveEndDate() != null) as.setEffectiveEndDate(dto.getEffectiveEndDate());
        if (dto.getStatus() != null) as.setStatus(dto.getStatus());
        if (dto.getNotes() != null) as.setNotes(dto.getNotes());

        BusAssignmentEntity updated = assignmentRepository.save(as);
        logAudit(sId, "ASSIGNMENT_UPDATED", "ASSIGNMENT", updated.getId(), "Updated assignment id: " + id);
        return mapToAssignmentDto(updated);
    }

    @Override
    public void deleteAssignment(Long id, String schoolId) {
        String sId = resolveSchool(schoolId);
        BusAssignmentEntity as = assignmentRepository.findByIdAndSchoolId(id, sId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + id));
        assignmentRepository.delete(as);
        logAudit(sId, "ASSIGNMENT_DELETED", "ASSIGNMENT", id, "Deleted assignment id: " + id);
    }

    private BusAssignmentDto mapToAssignmentDto(BusAssignmentEntity as) {
        BusAssignmentDto dto = BusAssignmentDto.builder()
                .id(as.getId())
                .schoolId(as.getSchoolId())
                .driverId(as.getDriverId())
                .busId(as.getBusId())
                .routeId(as.getRouteId())
                .shift(as.getShift())
                .effectiveStartDate(as.getEffectiveStartDate())
                .effectiveEndDate(as.getEffectiveEndDate())
                .status(as.getStatus())
                .notes(as.getNotes())
                .build();

        driverRepository.findById(as.getDriverId()).ifPresent(d -> {
            dto.setDriverName(d.getName());
            dto.setDriverPhone(d.getPhone());
        });

        busRepository.findById(as.getBusId()).ifPresent(b -> {
            dto.setBusNumber(b.getBusNumber());
            dto.setRegistrationNumber(b.getRegistrationNumber());
        });

        routeRepository.findById(as.getRouteId()).ifPresent(r -> {
            dto.setRouteName(r.getRouteName());
            dto.setRouteCode(r.getRouteCode());
        });

        return dto;
    }

    // ==========================================
    // STUDENT TRANSPORT ASSIGNMENTS
    // ==========================================

    @Override
    public List<StudentAssignmentDto> getStudentAssignments(String schoolId) {
        String sId = resolveSchool(schoolId);
        seedInitialTransportDataIfEmpty(sId);
        return studentAssignmentRepository.findBySchoolId(sId).stream()
                .map(this::mapToStudentAssignmentDto)
                .collect(Collectors.toList());
    }

    @Override
    public StudentAssignmentDto assignStudentToRoute(StudentAssignmentDto dto, String schoolId) {
        String sId = resolveSchool(schoolId);

        Double fee = dto.getMonthlyFee();
        if (fee == null || fee <= 0) {
            fee = routeRepository.findById(dto.getRouteId()).map(TransportRouteEntity::getFare).orElse(1500.0);
            if ("ONE_WAY".equalsIgnoreCase(dto.getTripType())) {
                fee = fee * 0.6;
            }
        }

        StudentTransportAssignmentEntity entity = StudentTransportAssignmentEntity.builder()
                .schoolId(sId)
                .studentId(dto.getStudentId())
                .studentName(dto.getStudentName())
                .admissionNo(dto.getAdmissionNo())
                .className(dto.getClassName())
                .routeId(dto.getRouteId())
                .pickupStopId(dto.getPickupStopId())
                .dropStopId(dto.getDropStopId())
                .pickupTime(dto.getPickupTime())
                .dropTime(dto.getDropTime())
                .guardianPhone(dto.getGuardianPhone())
                .tripType(dto.getTripType() != null ? dto.getTripType() : "BOTH_WAY")
                .startDate(dto.getStartDate() != null ? dto.getStartDate() : LocalDate.now())
                .endDate(dto.getEndDate())
                .monthlyFee(fee)
                .feeStatus(dto.getFeeStatus() != null ? dto.getFeeStatus() : "PENDING")
                .status("ACTIVE")
                .pickupStatus("SCHEDULED")
                .build();

        StudentTransportAssignmentEntity saved = studentAssignmentRepository.save(entity);
        logAudit(sId, "STUDENT_ASSIGNED", "STUDENT", saved.getStudentId(), "Assigned student to route " + saved.getRouteId());
        return mapToStudentAssignmentDto(saved);
    }

    @Override
    public StudentAssignmentDto updateStudentAssignment(Long id, StudentAssignmentDto dto, String schoolId) {
        String sId = resolveSchool(schoolId);
        StudentTransportAssignmentEntity entity = studentAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student assignment not found with id: " + id));

        if (dto.getStudentName() != null) entity.setStudentName(dto.getStudentName());
        if (dto.getAdmissionNo() != null) entity.setAdmissionNo(dto.getAdmissionNo());
        if (dto.getClassName() != null) entity.setClassName(dto.getClassName());
        if (dto.getRouteId() != null) entity.setRouteId(dto.getRouteId());
        if (dto.getPickupStopId() != null) entity.setPickupStopId(dto.getPickupStopId());
        if (dto.getDropStopId() != null) entity.setDropStopId(dto.getDropStopId());
        if (dto.getPickupTime() != null) entity.setPickupTime(dto.getPickupTime());
        if (dto.getDropTime() != null) entity.setDropTime(dto.getDropTime());
        if (dto.getGuardianPhone() != null) entity.setGuardianPhone(dto.getGuardianPhone());
        if (dto.getTripType() != null) entity.setTripType(dto.getTripType());
        if (dto.getStartDate() != null) entity.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) entity.setEndDate(dto.getEndDate());
        if (dto.getMonthlyFee() != null) entity.setMonthlyFee(dto.getMonthlyFee());
        if (dto.getFeeStatus() != null) entity.setFeeStatus(dto.getFeeStatus());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        if (dto.getPickupStatus() != null) entity.setPickupStatus(dto.getPickupStatus());

        StudentTransportAssignmentEntity saved = studentAssignmentRepository.save(entity);
        logAudit(sId, "STUDENT_ASSIGNMENT_UPDATED", "STUDENT", saved.getStudentId(), "Updated student transport assignment " + id);
        return mapToStudentAssignmentDto(saved);
    }

    @Override
    public void removeStudentAssignment(Long id, String schoolId) {
        String sId = resolveSchool(schoolId);
        studentAssignmentRepository.deleteById(id);
        logAudit(sId, "STUDENT_UNASSIGNED", "STUDENT", id, "Removed student assignment id " + id);
    }

    @Override
    public void updateStudentPickupStatus(Long assignmentId, String status, String schoolId) {
        studentAssignmentRepository.findById(assignmentId).ifPresent(s -> {
            s.setPickupStatus(status);
            s.setLastStatusUpdate(LocalDateTime.now());
            studentAssignmentRepository.save(s);
        });
    }

    private StudentAssignmentDto mapToStudentAssignmentDto(StudentTransportAssignmentEntity s) {
        List<TransportFeePaymentEntity> payments = feePaymentRepository.findByAssignmentId(s.getId());
        double paid = payments.stream().mapToDouble(p -> p.getAmountPaid() != null ? p.getAmountPaid() : 0.0).sum();
        double fee = s.getMonthlyFee() != null ? s.getMonthlyFee() : 0.0;
        double pending = Math.max(0.0, fee - paid);

        String currentFeeStatus = s.getFeeStatus();
        if (paid >= fee && fee > 0) {
            currentFeeStatus = "PAID";
        } else if (paid > 0 && paid < fee) {
            currentFeeStatus = "PARTIAL";
        } else if (currentFeeStatus == null || currentFeeStatus.isBlank()) {
            currentFeeStatus = "PENDING";
        }

        StudentAssignmentDto dto = StudentAssignmentDto.builder()
                .id(s.getId())
                .schoolId(s.getSchoolId())
                .studentId(s.getStudentId())
                .studentName(s.getStudentName())
                .admissionNo(s.getAdmissionNo())
                .className(s.getClassName())
                .routeId(s.getRouteId())
                .pickupStopId(s.getPickupStopId())
                .dropStopId(s.getDropStopId())
                .pickupTime(s.getPickupTime())
                .dropTime(s.getDropTime())
                .guardianPhone(s.getGuardianPhone())
                .tripType(s.getTripType() != null ? s.getTripType() : "BOTH_WAY")
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .monthlyFee(s.getMonthlyFee())
                .feeStatus(currentFeeStatus)
                .paidAmount(paid)
                .pendingAmount(pending)
                .status(s.getStatus())
                .pickupStatus(s.getPickupStatus())
                .build();

        routeRepository.findById(s.getRouteId()).ifPresent(r -> dto.setRouteName(r.getRouteName()));
        if (s.getPickupStopId() != null) {
            stopRepository.findById(s.getPickupStopId()).ifPresent(st -> dto.setPickupStopName(st.getStopName()));
        }
        if (s.getDropStopId() != null) {
            stopRepository.findById(s.getDropStopId()).ifPresent(st -> dto.setDropStopName(st.getStopName()));
        }
        return dto;
    }

    // ==========================================
    // TRANSPORT FEES & PAYMENTS
    // ==========================================

    @Override
    public TransportFeeSummaryDto getFeeSummary(String schoolId) {
        String sId = resolveSchool(schoolId);
        seedInitialTransportDataIfEmpty(sId);

        List<StudentTransportAssignmentEntity> assignments = studentAssignmentRepository.findBySchoolId(sId);
        List<StudentAssignmentDto> studentRecords = assignments.stream()
                .map(this::mapToStudentAssignmentDto)
                .collect(Collectors.toList());

        List<TransportFeePaymentEntity> paymentEntities = feePaymentRepository.findBySchoolIdOrderByPaymentDateDesc(sId);
        List<TransportPaymentDto> recentPayments = paymentEntities.stream()
                .map(this::mapToPaymentDto)
                .collect(Collectors.toList());

        double totalAssessed = studentRecords.stream()
                .mapToDouble(s -> s.getMonthlyFee() != null ? s.getMonthlyFee() : 0.0)
                .sum();
        double totalCollected = recentPayments.stream()
                .mapToDouble(p -> p.getAmountPaid() != null ? p.getAmountPaid() : 0.0)
                .sum();
        double totalPending = Math.max(0.0, totalAssessed - totalCollected);
        double totalOverdue = studentRecords.stream()
                .filter(s -> "OVERDUE".equalsIgnoreCase(s.getFeeStatus()))
                .mapToDouble(s -> s.getPendingAmount() != null ? s.getPendingAmount() : 0.0)
                .sum();

        long paidCount = studentRecords.stream().filter(s -> "PAID".equalsIgnoreCase(s.getFeeStatus())).count();
        long pendingCount = studentRecords.stream().filter(s -> "PENDING".equalsIgnoreCase(s.getFeeStatus()) || "PARTIAL".equalsIgnoreCase(s.getFeeStatus())).count();
        long overdueCount = studentRecords.stream().filter(s -> "OVERDUE".equalsIgnoreCase(s.getFeeStatus())).count();

        return TransportFeeSummaryDto.builder()
                .totalAssessed(totalAssessed)
                .totalCollected(totalCollected)
                .totalPending(totalPending)
                .totalOverdue(totalOverdue)
                .paidCount(paidCount)
                .pendingCount(pendingCount)
                .overdueCount(overdueCount)
                .studentRecords(studentRecords)
                .recentPayments(recentPayments)
                .build();
    }

    @Override
    public TransportPaymentDto collectFee(TransportPaymentDto dto, String schoolId) {
        String sId = resolveSchool(schoolId);

        String receiptNo = (dto.getReceiptNumber() != null && !dto.getReceiptNumber().isBlank())
                ? dto.getReceiptNumber()
                : "TR-REC-" + System.currentTimeMillis();

        TransportFeePaymentEntity payment = TransportFeePaymentEntity.builder()
                .schoolId(sId)
                .assignmentId(dto.getAssignmentId())
                .studentId(dto.getStudentId())
                .studentName(dto.getStudentName())
                .admissionNo(dto.getAdmissionNo())
                .receiptNumber(receiptNo)
                .amountPaid(dto.getAmountPaid() != null ? dto.getAmountPaid() : 0.0)
                .paymentDate(dto.getPaymentDate() != null ? dto.getPaymentDate() : LocalDate.now())
                .paymentMode(dto.getPaymentMode() != null ? dto.getPaymentMode() : "CASH")
                .feeMonth(dto.getFeeMonth() != null ? dto.getFeeMonth() : LocalDate.now().getMonth().name())
                .remarks(dto.getRemarks())
                .receivedBy(dto.getReceivedBy() != null ? dto.getReceivedBy() : "Admin Office")
                .build();

        TransportFeePaymentEntity saved = feePaymentRepository.save(payment);

        if (dto.getAssignmentId() != null) {
            studentAssignmentRepository.findById(dto.getAssignmentId()).ifPresent(as -> {
                List<TransportFeePaymentEntity> allPaid = feePaymentRepository.findByAssignmentId(as.getId());
                double totalPaid = allPaid.stream().mapToDouble(p -> p.getAmountPaid() != null ? p.getAmountPaid() : 0.0).sum();
                double fee = as.getMonthlyFee() != null ? as.getMonthlyFee() : 0.0;
                if (totalPaid >= fee && fee > 0) {
                    as.setFeeStatus("PAID");
                } else if (totalPaid > 0) {
                    as.setFeeStatus("PARTIAL");
                } else {
                    as.setFeeStatus("PENDING");
                }
                studentAssignmentRepository.save(as);
            });
        }

        logAudit(sId, "TRANSPORT_FEE_COLLECTED", "PAYMENT", saved.getId(), "Collected transport fee of Rs. " + saved.getAmountPaid() + " from " + saved.getStudentName());
        return mapToPaymentDto(saved);
    }

    private TransportPaymentDto mapToPaymentDto(TransportFeePaymentEntity p) {
        return TransportPaymentDto.builder()
                .id(p.getId())
                .schoolId(p.getSchoolId())
                .assignmentId(p.getAssignmentId())
                .studentId(p.getStudentId())
                .studentName(p.getStudentName())
                .admissionNo(p.getAdmissionNo())
                .receiptNumber(p.getReceiptNumber())
                .amountPaid(p.getAmountPaid())
                .paymentDate(p.getPaymentDate())
                .paymentMode(p.getPaymentMode())
                .feeMonth(p.getFeeMonth())
                .remarks(p.getRemarks())
                .receivedBy(p.getReceivedBy())
                .build();
    }

    // ==========================================
    // DRIVER APP CORE OPERATIONS
    // ==========================================

    private DriverProfileEntity resolveDriver(String driverEmail) {
        if (driverEmail != null && !driverEmail.isBlank()) {
            String cleanEmail = driverEmail.trim();
            Optional<DriverProfileEntity> byEmail = driverRepository.findByEmailIgnoreCase(cleanEmail);
            if (byEmail.isPresent()) return byEmail.get();

            Optional<UserEntity> userOpt = userRepository.findByEmailIgnoreCase(cleanEmail);
            if (userOpt.isPresent()) {
                UserEntity user = userOpt.get();
                List<BusEntity> allBuses = busRepository.findAll();
                Long busId = allBuses.isEmpty() ? null : allBuses.get(0).getId();
                DriverProfileEntity newDriver = DriverProfileEntity.builder()
                        .schoolId(DEFAULT_SCHOOL_ID)
                        .userId(user.getId())
                        .name(user.getName() != null && !user.getName().isBlank() ? user.getName() : "Driver")
                        .email(user.getEmail())
                        .phone("+91 98765 43210")
                        .licenseNumber("DL-" + Math.abs(user.getEmail().hashCode() % 90000000 + 10000000))
                        .licenseExpiryDate(LocalDate.now().plusYears(4))
                        .licenseCategory("COMMERCIAL_HEAVY")
                        .experienceYears(6)
                        .joiningDate(LocalDate.now().minusYears(1))
                        .verificationStatus("VERIFIED")
                        .status("ACTIVE")
                        .address("Campus Staff Quarters, Patna")
                        .assignedBusId(busId)
                        .build();
                return driverRepository.save(newDriver);
            }
        }
        Optional<DriverProfileEntity> defaultDriver = driverRepository.findByEmailIgnoreCase("driver@school.com");
        if (defaultDriver.isPresent()) return defaultDriver.get();

        List<DriverProfileEntity> all = driverRepository.findBySchoolId("SCH-001");
        if (!all.isEmpty()) return all.get(0);

        List<DriverProfileEntity> any = driverRepository.findAll();
        if (!any.isEmpty()) return any.get(0);

        seedInitialTransportDataIfEmpty("SCH-001");
        return driverRepository.findByEmailIgnoreCase("driver@school.com")
                .orElseThrow(() -> new ResourceNotFoundException("No driver found"));
    }

    @Override
    public TripDtos.DriverDutyDto getDriverDuty(String driverEmail) {
        DriverProfileEntity driver = resolveDriver(driverEmail);
        seedInitialTransportDataIfEmpty(driver.getSchoolId());

        BusAssignmentEntity assignment = assignmentRepository.findFirstByDriverIdAndStatusOrderByIdDesc(driver.getId(), "ACTIVE")
                .orElse(null);

        if (assignment == null) {
            // Auto-assign first available bus and route to guarantee dynamic duty data
            List<TransportRouteEntity> routes = routeRepository.findBySchoolId(driver.getSchoolId());
            if (routes.isEmpty()) routes = routeRepository.findBySchoolId(DEFAULT_SCHOOL_ID);
            if (routes.isEmpty()) routes = routeRepository.findAll();

            List<BusEntity> buses = busRepository.findBySchoolId(driver.getSchoolId());
            if (buses.isEmpty()) buses = busRepository.findBySchoolId(DEFAULT_SCHOOL_ID);
            if (buses.isEmpty()) buses = busRepository.findAll();

            if (!routes.isEmpty() && !buses.isEmpty()) {
                BusAssignmentEntity autoAssignment = BusAssignmentEntity.builder()
                        .schoolId(driver.getSchoolId() != null ? driver.getSchoolId() : DEFAULT_SCHOOL_ID)
                        .driverId(driver.getId())
                        .routeId(routes.get(0).getId())
                        .busId(buses.get(0).getId())
                        .shift("BOTH")
                        .effectiveStartDate(LocalDate.now())
                        .status("ACTIVE")
                        .notes("Daily scheduled bus assignment")
                        .build();
                assignment = assignmentRepository.save(autoAssignment);
            }
        }

        TripDtos.DriverDutyDto.DriverDutyDtoBuilder dutyBuilder = TripDtos.DriverDutyDto.builder()
                .driverId(driver.getId())
                .driverName(driver.getName())
                .phone(driver.getPhone())
                .licenseNumber(driver.getLicenseNumber())
                .licenseExpiryDate(driver.getLicenseExpiryDate() != null ? driver.getLicenseExpiryDate().toString() : "2029-06-30")
                .tripStatus("NONE");

        if (assignment != null) {
            dutyBuilder.assignmentId(assignment.getId())
                    .busId(assignment.getBusId())
                    .routeId(assignment.getRouteId())
                    .shift(assignment.getShift())
                    .tripStatus("SCHEDULED");

            busRepository.findById(assignment.getBusId()).ifPresent(b -> {
                dutyBuilder.busNumber(b.getBusNumber())
                        .registrationNumber(b.getRegistrationNumber())
                        .busCapacity(b.getCapacity());
            });

            routeRepository.findById(assignment.getRouteId()).ifPresent(r -> {
                dutyBuilder.routeName(r.getRouteName())
                        .routeCode(r.getRouteCode())
                        .plannedDepartureTime(r.getMorningStartTime() != null ? r.getMorningStartTime() : "07:15 AM")
                        .route(mapToRouteDto(r));
            });
        }

        // Check if there is an in-progress or completed trip today
        tripRepository.findBySchoolId(driver.getSchoolId()).stream()
                .filter(t -> t.getDriverId().equals(driver.getId()))
                .filter(t -> LocalDate.now().equals(t.getTripDate()))
                .max(Comparator.comparing(TransportTripEntity::getId))
                .ifPresent(trip -> {
                    dutyBuilder.todayChecklistCompleted(trip.getPreTripCheckPassed());
                    dutyBuilder.todayChecklistNotes(trip.getPreTripNotes());
                    dutyBuilder.todayStartLatitude(trip.getStartLatitude());
                    dutyBuilder.todayStartLongitude(trip.getStartLongitude());
                    dutyBuilder.todayStartLocationName(trip.getStartLocationName());
                    dutyBuilder.todayActualStartTime(trip.getActualStartTime());
                    dutyBuilder.todayChkBrakes(trip.getChkBrakes());
                    dutyBuilder.todayChkLights(trip.getChkLights());
                    dutyBuilder.todayChkTires(trip.getChkTires());
                    dutyBuilder.todayChkEmergencyKit(trip.getChkEmergencyKit());
                    dutyBuilder.todayChkGpsActive(trip.getChkGpsActive());
                    if ("IN_PROGRESS".equals(trip.getStatus())) {
                        dutyBuilder.activeTripId(trip.getId())
                                .tripStatus("IN_PROGRESS")
                                .activeTrip(mapToTripSummaryDto(trip));
                    } else if ("COMPLETED".equals(trip.getStatus())) {
                        dutyBuilder.tripStatus("COMPLETED");
                    }
                });

        return dutyBuilder.build();
    }

    @Override
    public TripDtos.TripSummaryDto startTrip(String driverEmail, TripDtos.StartTripRequest request) {
        DriverProfileEntity driver = resolveDriver(driverEmail);

        // Idempotency: check if driver already has an in-progress trip
        Optional<TransportTripEntity> existingActive = tripRepository.findFirstByDriverIdAndStatus(driver.getId(), "IN_PROGRESS");
        if (existingActive.isPresent()) {
            return mapToTripSummaryDto(existingActive.get());
        }

        BusAssignmentEntity assignment = null;
        if (request.getAssignmentId() != null) {
            assignment = assignmentRepository.findById(request.getAssignmentId()).orElse(null);
        }
        if (assignment == null) {
            assignment = assignmentRepository.findFirstByDriverIdAndStatusOrderByIdDesc(driver.getId(), "ACTIVE")
                    .orElse(null);
        }
        if (assignment == null) {
            List<TransportRouteEntity> routes = routeRepository.findBySchoolId(driver.getSchoolId());
            if (routes.isEmpty()) routes = routeRepository.findBySchoolId(DEFAULT_SCHOOL_ID);
            if (routes.isEmpty()) routes = routeRepository.findAll();

            List<BusEntity> buses = busRepository.findBySchoolId(driver.getSchoolId());
            if (buses.isEmpty()) buses = busRepository.findBySchoolId(DEFAULT_SCHOOL_ID);
            if (buses.isEmpty()) buses = busRepository.findAll();

            Long rId = (!routes.isEmpty()) ? routes.get(0).getId() : 1L;
            Long bId = (!buses.isEmpty()) ? buses.get(0).getId() : 1L;
            BusAssignmentEntity autoAssignment = BusAssignmentEntity.builder()
                    .schoolId(driver.getSchoolId() != null ? driver.getSchoolId() : DEFAULT_SCHOOL_ID)
                    .driverId(driver.getId())
                    .routeId(rId)
                    .busId(bId)
                    .shift(request.getShift() != null ? request.getShift() : "MORNING")
                    .status("ACTIVE")
                    .effectiveStartDate(LocalDate.now())
                    .notes("Primary daily route assignment")
                    .build();
            assignment = assignmentRepository.save(autoAssignment);
        }

        Long routeId = request.getRouteId() != null ? request.getRouteId() : assignment.getRouteId();
        Long busId = request.getBusId() != null ? request.getBusId() : assignment.getBusId();

        List<RouteStopEntity> stops = stopRepository.findByRouteIdOrderByStopOrderAsc(routeId);
        Long firstStopId = stops.isEmpty() ? null : stops.get(0).getId();

        Double startLat = request.getStartLatitude() != null ? request.getStartLatitude() : (stops.isEmpty() ? 25.6150 : stops.get(0).getLatitude());
        Double startLon = request.getStartLongitude() != null ? request.getStartLongitude() : (stops.isEmpty() ? 85.1280 : stops.get(0).getLongitude());
        String startLocName = (request.getStartLocationName() != null && !request.getStartLocationName().isBlank())
                ? request.getStartLocationName()
                : (stops.isEmpty() ? "Patna Campus Terminal" : stops.get(0).getStopName());

        TransportTripEntity trip = TransportTripEntity.builder()
                .schoolId(driver.getSchoolId())
                .busAssignmentId(assignment.getId())
                .driverId(driver.getId())
                .busId(busId)
                .routeId(routeId)
                .shift(request.getShift() != null ? request.getShift() : "MORNING")
                .tripDate(LocalDate.now())
                .scheduledStartTime(LocalDateTime.now())
                .actualStartTime(LocalDateTime.now())
                .status("IN_PROGRESS")
                .totalDistanceKm(0.0)
                .currentSpeedKmh(0.0)
                .startLatitude(startLat)
                .startLongitude(startLon)
                .startLocationName(startLocName)
                .lastLatitude(startLat)
                .lastLongitude(startLon)
                .lastLocationTime(LocalDateTime.now())
                .nextStopId(firstStopId)
                .nextStopEtaMinutes(10)
                .completedStopsCount(0)
                .totalStopsCount(stops.size())
                .preTripCheckPassed(Boolean.TRUE.equals(request.getPreTripCheckPassed()))
                .preTripNotes(request.getPreTripNotes() != null ? request.getPreTripNotes() : "Daily 5-point vehicle safety inspection passed.")
                .chkBrakes(request.getChkBrakes() != null ? request.getChkBrakes() : true)
                .chkLights(request.getChkLights() != null ? request.getChkLights() : true)
                .chkTires(request.getChkTires() != null ? request.getChkTires() : true)
                .chkEmergencyKit(request.getChkEmergencyKit() != null ? request.getChkEmergencyKit() : true)
                .chkGpsActive(request.getChkGpsActive() != null ? request.getChkGpsActive() : true)
                .build();

        TransportTripEntity saved = tripRepository.save(trip);

        // Record start location point in history
        BusLocationHistoryEntity firstLoc = BusLocationHistoryEntity.builder()
                .schoolId(driver.getSchoolId())
                .tripId(saved.getId())
                .busId(busId)
                .driverId(driver.getId())
                .latitude(saved.getLastLatitude())
                .longitude(saved.getLastLongitude())
                .speedKmh(0.0)
                .recordedAt(LocalDateTime.now())
                .serverReceivedAt(LocalDateTime.now())
                .build();
        locationHistoryRepository.save(firstLoc);

        // Update bus status
        busRepository.findById(busId).ifPresent(b -> {
            b.setLastLatitude(saved.getLastLatitude());
            b.setLastLongitude(saved.getLastLongitude());
            b.setCurrentSpeed(0.0);
            b.setLastGpsUpdate(LocalDateTime.now());
            busRepository.save(b);
        });

        logAudit(driver.getSchoolId(), "TRIP_STARTED", "TRIP", saved.getId(), "Driver started trip id: " + saved.getId() + " from: " + startLocName);
        return mapToTripSummaryDto(saved);
    }

    @Override
    public int ingestGpsBatch(Long tripId, String driverEmail, TripDtos.BatchGpsRequest request) {
        if (request.getPoints() == null || request.getPoints().isEmpty()) {
            return 0;
        }

        TransportTripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        if (!"IN_PROGRESS".equalsIgnoreCase(trip.getStatus())) {
            throw new IllegalStateException("Cannot upload GPS points for trip with status: " + trip.getStatus());
        }

        TransportRouteEntity route = routeRepository.findById(trip.getRouteId()).orElse(null);
        int speedLimit = route != null && route.getSpeedLimitKmh() != null ? route.getSpeedLimitKmh() : 50;

        List<BusLocationHistoryEntity> historyBatch = new ArrayList<>();
        double accumulatedDistance = 0.0;
        Double prevLat = trip.getLastLatitude();
        Double prevLon = trip.getLastLongitude();

        TripDtos.GpsPointDto latestPoint = null;

        for (TripDtos.GpsPointDto p : request.getPoints()) {
            if (p.getLatitude() == null || p.getLongitude() == null) continue;

            latestPoint = p;

            if (prevLat != null && prevLon != null) {
                accumulatedDistance += calculateDistanceKm(prevLat, prevLon, p.getLatitude(), p.getLongitude());
            }
            prevLat = p.getLatitude();
            prevLon = p.getLongitude();

            historyBatch.add(BusLocationHistoryEntity.builder()
                    .schoolId(trip.getSchoolId())
                    .tripId(tripId)
                    .busId(trip.getBusId())
                    .driverId(trip.getDriverId())
                    .latitude(p.getLatitude())
                    .longitude(p.getLongitude())
                    .speedKmh(p.getSpeedKmh() != null ? p.getSpeedKmh() : 0.0)
                    .heading(p.getHeading())
                    .accuracyMeters(p.getAccuracyMeters() != null ? p.getAccuracyMeters() : 5.0)
                    .recordedAt(p.getRecordedAt() != null ? p.getRecordedAt() : LocalDateTime.now())
                    .serverReceivedAt(LocalDateTime.now())
                    .build());

            // Automated rule 1: Overspeed detection
            if (p.getSpeedKmh() != null && p.getSpeedKmh() > (speedLimit + 5)) {
                createOverspeedAlert(trip, p.getSpeedKmh(), speedLimit);
            }
        }

        if (!historyBatch.isEmpty()) {
            locationHistoryRepository.saveAll(historyBatch);
        }

        if (latestPoint != null) {
            trip.setLastLatitude(latestPoint.getLatitude());
            trip.setLastLongitude(latestPoint.getLongitude());
            trip.setCurrentSpeedKmh(latestPoint.getSpeedKmh() != null ? latestPoint.getSpeedKmh() : 0.0);
            trip.setLastLocationTime(LocalDateTime.now());
            trip.setTotalDistanceKm(trip.getTotalDistanceKm() + accumulatedDistance);

            // Automated rule 2: Geofence stop proximity check
            checkStopGeofences(trip, latestPoint.getLatitude(), latestPoint.getLongitude());

            tripRepository.save(trip);

            // Update bus live coordinates
            busRepository.findById(trip.getBusId()).ifPresent(b -> {
                b.setLastLatitude(trip.getLastLatitude());
                b.setLastLongitude(trip.getLastLongitude());
                b.setCurrentSpeed(trip.getCurrentSpeedKmh());
                b.setLastGpsUpdate(LocalDateTime.now());
                busRepository.save(b);
            });
        }

        return historyBatch.size();
    }

    private void checkStopGeofences(TransportTripEntity trip, double currentLat, double currentLon) {
        List<RouteStopEntity> stops = stopRepository.findByRouteIdOrderByStopOrderAsc(trip.getRouteId());
        for (RouteStopEntity stop : stops) {
            double distMeters = calculateDistanceKm(currentLat, currentLon, stop.getLatitude(), stop.getLongitude()) * 1000;
            int radius = stop.getGeofenceRadiusMeters() != null ? stop.getGeofenceRadiusMeters() : 50;

            if (distMeters <= (radius + 20)) {
                // If close to this stop, mark as current
                trip.setCurrentStopId(stop.getId());
                // Find next stop
                int currentIndex = stops.indexOf(stop);
                if (currentIndex + 1 < stops.size()) {
                    trip.setNextStopId(stops.get(currentIndex + 1).getId());
                    trip.setNextStopEtaMinutes(Math.max(2, (int) Math.round(distMeters / 300)));
                } else {
                    trip.setNextStopId(null);
                    trip.setNextStopEtaMinutes(0);
                }
                break;
            }
        }
    }

    private void createOverspeedAlert(TransportTripEntity trip, double speed, int limit) {
        TransportAlertEntity alert = TransportAlertEntity.builder()
                .schoolId(trip.getSchoolId())
                .tripId(trip.getId())
                .busId(trip.getBusId())
                .alertType("OVERSPEED")
                .severity("CRITICAL")
                .title("Speed Limit Exceeded (" + Math.round(speed) + " km/h)")
                .message("Bus speed of " + Math.round(speed) + " km/h exceeds configured route limit of " + limit + " km/h.")
                .status("OPEN")
                .build();
        alertRepository.save(alert);
    }

    @Override
    public TripDtos.StopEventDto recordStopEvent(Long tripId, Long stopId, String driverEmail, TripDtos.StopEventRequest request) {
        TransportTripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        RouteStopEntity stop = stopRepository.findById(stopId)
                .orElseThrow(() -> new ResourceNotFoundException("Stop not found"));

        TripStopEventEntity event = stopEventRepository.findByTripIdAndStopId(tripId, stopId)
                .orElse(TripStopEventEntity.builder()
                        .schoolId(trip.getSchoolId())
                        .tripId(tripId)
                        .stopId(stopId)
                        .stopName(stop.getStopName())
                        .stopOrder(stop.getStopOrder())
                        .arrivalTime(LocalDateTime.now())
                        .isPlanned(true)
                        .status("ARRIVED")
                        .build());

        String eventType = request.getEventType() != null ? request.getEventType() : "ARRIVED";
        event.setStatus(eventType);

        if ("DEPARTED".equalsIgnoreCase(eventType)) {
            event.setDepartureTime(LocalDateTime.now());
            if (event.getArrivalTime() != null) {
                long dwellSec = Duration.between(event.getArrivalTime(), event.getDepartureTime()).getSeconds();
                event.setDwellDurationSeconds(dwellSec);
                if (dwellSec > 300) { // Long stop alert threshold: 5 mins
                    TransportAlertEntity alert = TransportAlertEntity.builder()
                            .schoolId(trip.getSchoolId())
                            .tripId(tripId)
                            .busId(trip.getBusId())
                            .alertType("LONG_STOP")
                            .severity("WARNING")
                            .title("Long Dwell Time at " + stop.getStopName())
                            .message("Bus dwelled for " + (dwellSec / 60) + " minutes at stop: " + stop.getStopName())
                            .status("OPEN")
                            .build();
                    alertRepository.save(alert);
                }
            }
            trip.setCompletedStopsCount(trip.getCompletedStopsCount() + 1);
            tripRepository.save(trip);
        }

        if (request.getStudentsBoarded() != null) {
            event.setStudentsBoardedCount(request.getStudentsBoarded());
        }
        if (request.getStudentsDeboarded() != null) {
            event.setStudentsDeboardedCount(request.getStudentsDeboarded());
        }
        if (request.getNotes() != null) {
            event.setNotes(request.getNotes());
        }

        TripStopEventEntity saved = stopEventRepository.save(event);

        return TripDtos.StopEventDto.builder()
                .id(saved.getId())
                .stopId(saved.getStopId())
                .stopName(saved.getStopName())
                .stopOrder(saved.getStopOrder())
                .arrivalTime(saved.getArrivalTime())
                .departureTime(saved.getDepartureTime())
                .dwellDurationSeconds(saved.getDwellDurationSeconds())
                .status(saved.getStatus())
                .studentsBoarded(saved.getStudentsBoardedCount())
                .studentsDeboarded(saved.getStudentsDeboardedCount())
                .build();
    }

    @Override
    public TripDtos.TripIncidentDto reportIncident(Long tripId, String driverEmail, TripDtos.TripIncidentRequest request) {
        TransportTripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        TripIncidentEntity incident = TripIncidentEntity.builder()
                .schoolId(trip.getSchoolId())
                .tripId(tripId)
                .driverId(trip.getDriverId())
                .busId(trip.getBusId())
                .incidentType(request.getIncidentType() != null ? request.getIncidentType() : "OTHER")
                .severity(request.getSeverity() != null ? request.getSeverity() : "HIGH")
                .description(request.getDescription())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .reportedAt(LocalDateTime.now())
                .status("OPEN")
                .build();

        TripIncidentEntity saved = incidentRepository.save(incident);

        // Raise priority alert for admin
        TransportAlertEntity alert = TransportAlertEntity.builder()
                .schoolId(trip.getSchoolId())
                .tripId(tripId)
                .busId(trip.getBusId())
                .alertType(request.getIncidentType())
                .severity(request.getSeverity() != null ? request.getSeverity() : "CRITICAL")
                .title("Incident Reported: " + request.getIncidentType().replace("_", " "))
                .message(request.getDescription())
                .status("OPEN")
                .build();
        alertRepository.save(alert);

        logAudit(trip.getSchoolId(), "INCIDENT_REPORTED", "INCIDENT", saved.getId(), "Incident: " + request.getIncidentType());

        return TripDtos.TripIncidentDto.builder()
                .id(saved.getId())
                .incidentType(saved.getIncidentType())
                .severity(saved.getSeverity())
                .description(saved.getDescription())
                .latitude(saved.getLatitude())
                .longitude(saved.getLongitude())
                .reportedAt(saved.getReportedAt())
                .status(saved.getStatus())
                .build();
    }

    @Override
    public TripDtos.TripSummaryDto endTrip(Long tripId, String driverEmail, TripDtos.EndTripRequest request) {
        TransportTripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        trip.setStatus("COMPLETED");
        trip.setActualEndTime(LocalDateTime.now());
        trip.setCurrentSpeedKmh(0.0);
        if (request.getTotalDistanceKm() != null && request.getTotalDistanceKm() > 0) {
            trip.setTotalDistanceKm(request.getTotalDistanceKm());
        }
        if (request.getNotes() != null) {
            trip.setEndNotes(request.getNotes());
        }

        TransportTripEntity saved = tripRepository.save(trip);

        busRepository.findById(trip.getBusId()).ifPresent(b -> {
            b.setCurrentSpeed(0.0);
            busRepository.save(b);
        });

        logAudit(trip.getSchoolId(), "TRIP_COMPLETED", "TRIP", saved.getId(), "Driver completed trip id: " + saved.getId());
        return mapToTripSummaryDto(saved);
    }

    // ==========================================
    // ADMIN LIVE TRACKING & PLAYBACK
    // ==========================================

    @Override
    public List<TripDtos.LiveTrackingBusDto> getLiveTracking(String schoolId) {
        String sId = resolveSchool(schoolId);
        seedInitialTransportDataIfEmpty(sId);

        List<BusEntity> buses = busRepository.findBySchoolId(sId);
        List<TripDtos.LiveTrackingBusDto> result = new ArrayList<>();

        for (BusEntity bus : buses) {
            Optional<TransportTripEntity> activeTrip = tripRepository.findFirstByBusIdAndStatus(bus.getId(), "IN_PROGRESS");
            BusAssignmentEntity as = assignmentRepository.findByBusIdAndStatus(bus.getId(), "ACTIVE").stream().findFirst().orElse(null);

            String tripStatus = activeTrip.isPresent() ? "IN_PROGRESS" : "IDLE";
            Long tripId = activeTrip.map(TransportTripEntity::getId).orElse(null);
            Long driverId = activeTrip.map(TransportTripEntity::getDriverId).orElse(as != null ? as.getDriverId() : null);
            Long routeId = activeTrip.map(TransportTripEntity::getRouteId).orElse(as != null ? as.getRouteId() : null);

            String driverName = "Unassigned";
            String driverPhone = "";
            if (driverId != null) {
                DriverProfileEntity d = driverRepository.findById(driverId).orElse(null);
                if (d != null) {
                    driverName = d.getName();
                    driverPhone = d.getPhone();
                }
            }

            String routeName = "No Route";
            Double totalRouteDist = 15.0;
            if (routeId != null) {
                TransportRouteEntity r = routeRepository.findById(routeId).orElse(null);
                if (r != null) {
                    routeName = r.getRouteName();
                    totalRouteDist = r.getTotalDistanceKm();
                }
            }

            LocalDateTime lastGps = bus.getLastGpsUpdate() != null ? bus.getLastGpsUpdate() : LocalDateTime.now().minusMinutes(1);
            boolean isOnline = Duration.between(lastGps, LocalDateTime.now()).toMinutes() < 4;

            String currentStop = "Depot";
            String nextStop = "En Route";
            int eta = 5;
            int completed = 0;
            int totalStops = 5;

            if (activeTrip.isPresent()) {
                TransportTripEntity t = activeTrip.get();
                completed = t.getCompletedStopsCount();
                totalStops = t.getTotalStopsCount();
                eta = t.getNextStopEtaMinutes() != null ? t.getNextStopEtaMinutes() : 6;
                if (t.getCurrentStopId() != null) {
                    stopRepository.findById(t.getCurrentStopId()).ifPresent(st -> {});
                }
                if (t.getNextStopId() != null) {
                    stopRepository.findById(t.getNextStopId()).ifPresent(st -> {});
                }
            }

            result.add(TripDtos.LiveTrackingBusDto.builder()
                    .busId(bus.getId())
                    .busNumber(bus.getBusNumber())
                    .registrationNumber(bus.getRegistrationNumber())
                    .tripId(tripId)
                    .tripStatus(tripStatus)
                    .driverId(driverId)
                    .driverName(driverName)
                    .driverPhone(driverPhone)
                    .routeId(routeId)
                    .routeName(routeName)
                    .shift(as != null ? as.getShift() : "MORNING")
                    .currentLatitude(bus.getLastLatitude() != null ? bus.getLastLatitude() : 25.6020)
                    .currentLongitude(bus.getLastLongitude() != null ? bus.getLastLongitude() : 85.1420)
                    .currentSpeedKmh(bus.getCurrentSpeed() != null ? bus.getCurrentSpeed() : 28.0)
                    .heading(45.0)
                    .lastGpsUpdate(lastGps)
                    .isGpsOnline(isOnline)
                    .currentStopName(currentStop)
                    .nextStopName(nextStop)
                    .nextStopEtaMinutes(eta)
                    .completedStops(completed)
                    .totalStops(totalStops)
                    .traveledDistanceKm(activeTrip.map(TransportTripEntity::getTotalDistanceKm).orElse(0.0))
                    .totalRouteDistanceKm(totalRouteDist)
                    .openAlertsCount((int) alertRepository.countBySchoolIdAndStatus(sId, "OPEN"))
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public TripDtos.TripPlaybackDto getTripPlayback(Long tripId, String schoolId) {
        TransportTripEntity trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        BusEntity bus = busRepository.findById(trip.getBusId()).orElse(null);
        DriverProfileEntity driver = driverRepository.findById(trip.getDriverId()).orElse(null);
        TransportRouteEntity route = routeRepository.findById(trip.getRouteId()).orElse(null);

        List<BusLocationHistoryEntity> rawHistory = locationHistoryRepository.findByTripIdOrderByRecordedAtAsc(tripId);
        List<TripDtos.GpsPointDto> points = rawHistory.stream()
                .map(h -> TripDtos.GpsPointDto.builder()
                        .latitude(h.getLatitude())
                        .longitude(h.getLongitude())
                        .speedKmh(h.getSpeedKmh())
                        .heading(h.getHeading())
                        .accuracyMeters(h.getAccuracyMeters())
                        .recordedAt(h.getRecordedAt())
                        .build())
                .collect(Collectors.toList());

        List<RouteStopDto> plannedStops = stopRepository.findByRouteIdOrderByStopOrderAsc(trip.getRouteId()).stream()
                .map(this::mapToRouteStopDto)
                .collect(Collectors.toList());

        List<TripDtos.StopEventDto> events = stopEventRepository.findByTripIdOrderByStopOrderAsc(tripId).stream()
                .map(e -> TripDtos.StopEventDto.builder()
                        .id(e.getId())
                        .stopId(e.getStopId())
                        .stopName(e.getStopName())
                        .stopOrder(e.getStopOrder())
                        .arrivalTime(e.getArrivalTime())
                        .departureTime(e.getDepartureTime())
                        .dwellDurationSeconds(e.getDwellDurationSeconds())
                        .status(e.getStatus())
                        .studentsBoarded(e.getStudentsBoardedCount())
                        .studentsDeboarded(e.getStudentsDeboardedCount())
                        .build())
                .collect(Collectors.toList());

        List<TripDtos.TripIncidentDto> incidents = incidentRepository.findByTripId(tripId).stream()
                .map(i -> TripDtos.TripIncidentDto.builder()
                        .id(i.getId())
                        .incidentType(i.getIncidentType())
                        .severity(i.getSeverity())
                        .description(i.getDescription())
                        .latitude(i.getLatitude())
                        .longitude(i.getLongitude())
                        .reportedAt(i.getReportedAt())
                        .status(i.getStatus())
                        .adminRemarks(i.getAdminRemarks())
                        .build())
                .collect(Collectors.toList());

        long durationMins = 0;
        if (trip.getActualStartTime() != null) {
            LocalDateTime end = trip.getActualEndTime() != null ? trip.getActualEndTime() : LocalDateTime.now();
            durationMins = Duration.between(trip.getActualStartTime(), end).toMinutes();
        }

        return TripDtos.TripPlaybackDto.builder()
                .tripId(trip.getId())
                .busNumber(bus != null ? bus.getBusNumber() : "Bus")
                .driverName(driver != null ? driver.getName() : "Driver")
                .routeName(route != null ? route.getRouteName() : "Route")
                .tripDate(trip.getTripDate().toString())
                .startTime(trip.getActualStartTime() != null ? trip.getActualStartTime().toString() : "")
                .endTime(trip.getActualEndTime() != null ? trip.getActualEndTime().toString() : "")
                .totalDistanceKm(trip.getTotalDistanceKm())
                .durationMinutes(durationMins)
                .pathPoints(points)
                .plannedStops(plannedStops)
                .stopEvents(events)
                .incidents(incidents)
                .build();
    }

    @Override
    public List<TripDtos.TripSummaryDto> getTripHistory(String schoolId, LocalDate date, Long busId, Long driverId) {
        String sId = resolveSchool(schoolId);
        seedInitialTransportDataIfEmpty(sId);

        List<TransportTripEntity> trips = tripRepository.findBySchoolId(sId);
        return trips.stream()
                .filter(t -> date == null || date.equals(t.getTripDate()))
                .filter(t -> busId == null || busId.equals(t.getBusId()))
                .filter(t -> driverId == null || driverId.equals(t.getDriverId()))
                .sorted(Comparator.comparing(TransportTripEntity::getTripDate).reversed().thenComparing(TransportTripEntity::getId).reversed())
                .map(this::mapToTripSummaryDto)
                .collect(Collectors.toList());
    }

    private TripDtos.TripSummaryDto mapToTripSummaryDto(TransportTripEntity trip) {
        BusEntity bus = busRepository.findById(trip.getBusId()).orElse(null);
        DriverProfileEntity driver = driverRepository.findById(trip.getDriverId()).orElse(null);
        TransportRouteEntity route = routeRepository.findById(trip.getRouteId()).orElse(null);

        long durationMins = 0;
        if (trip.getActualStartTime() != null) {
            LocalDateTime end = trip.getActualEndTime() != null ? trip.getActualEndTime() : LocalDateTime.now();
            durationMins = Duration.between(trip.getActualStartTime(), end).toMinutes();
        }

        String curStopName = "Depot";
        String nextStopName = "Completed";
        if (trip.getCurrentStopId() != null) {
            stopRepository.findById(trip.getCurrentStopId()).ifPresent(s -> {});
        }
        if (trip.getNextStopId() != null) {
            stopRepository.findById(trip.getNextStopId()).ifPresent(s -> {});
        }

        int incidents = incidentRepository.findByTripId(trip.getId()).size();

        return TripDtos.TripSummaryDto.builder()
                .tripId(trip.getId())
                .busId(trip.getBusId())
                .busNumber(bus != null ? bus.getBusNumber() : "Bus")
                .registrationNumber(bus != null ? bus.getRegistrationNumber() : "")
                .driverId(trip.getDriverId())
                .driverName(driver != null ? driver.getName() : "Driver")
                .routeId(trip.getRouteId())
                .routeName(route != null ? route.getRouteName() : "Route")
                .routeCode(route != null ? route.getRouteCode() : "")
                .shift(trip.getShift())
                .tripDate(trip.getTripDate())
                .status(trip.getStatus())
                .actualStartTime(trip.getActualStartTime())
                .actualEndTime(trip.getActualEndTime())
                .durationMinutes(durationMins)
                .totalDistanceKm(trip.getTotalDistanceKm())
                .currentSpeedKmh(trip.getCurrentSpeedKmh())
                .lastLatitude(trip.getLastLatitude())
                .lastLongitude(trip.getLastLongitude())
                .lastLocationTime(trip.getLastLocationTime())
                .completedStopsCount(trip.getCompletedStopsCount())
                .totalStopsCount(trip.getTotalStopsCount())
                .currentStopName(curStopName)
                .nextStopName(nextStopName)
                .nextStopEtaMinutes(trip.getNextStopEtaMinutes())
                .incidentCount(incidents)
                .startLatitude(trip.getStartLatitude())
                .startLongitude(trip.getStartLongitude())
                .startLocationName(trip.getStartLocationName() != null ? trip.getStartLocationName()
                        : (trip.getStartLatitude() != null ? String.format("%.4f, %.4f", trip.getStartLatitude(), trip.getStartLongitude()) : "Depot Main Bay"))
                .preTripCheckPassed(trip.getPreTripCheckPassed())
                .preTripNotes(trip.getPreTripNotes())
                .chkBrakes(trip.getChkBrakes())
                .chkLights(trip.getChkLights())
                .chkTires(trip.getChkTires())
                .chkEmergencyKit(trip.getChkEmergencyKit())
                .chkGpsActive(trip.getChkGpsActive())
                .build();
    }

    // ==========================================
    // ALERTS CENTER
    // ==========================================

    @Override
    public List<TransportAlertDto> getAlerts(String schoolId, String status) {
        String sId = resolveSchool(schoolId);
        seedInitialTransportDataIfEmpty(sId);

        List<TransportAlertEntity> alerts = (status != null && !status.isBlank())
                ? alertRepository.findBySchoolIdAndStatusOrderByCreatedAtDesc(sId, status.toUpperCase())
                : alertRepository.findBySchoolIdOrderByCreatedAtDesc(sId);

        return alerts.stream().map(a -> TransportAlertDto.builder()
                .id(a.getId())
                .tripId(a.getTripId())
                .busId(a.getBusId())
                .busNumber(a.getBusNumber())
                .alertType(a.getAlertType())
                .severity(a.getSeverity())
                .title(a.getTitle())
                .message(a.getMessage())
                .status(a.getStatus())
                .acknowledgedBy(a.getAcknowledgedBy())
                .acknowledgedAt(a.getAcknowledgedAt())
                .resolvedAt(a.getResolvedAt())
                .resolutionRemarks(a.getResolutionRemarks())
                .createdAt(a.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    @Override
    public TransportAlertDto acknowledgeAlert(Long alertId, String userEmail, String remarks, String schoolId) {
        TransportAlertEntity alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + alertId));

        alert.setStatus("ACKNOWLEDGED");
        alert.setAcknowledgedBy(userEmail);
        alert.setAcknowledgedAt(LocalDateTime.now());
        if (remarks != null && !remarks.isBlank()) {
            alert.setResolutionRemarks(remarks);
        }

        TransportAlertEntity saved = alertRepository.save(alert);
        logAudit(alert.getSchoolId(), "ALERT_ACKNOWLEDGED", "ALERT", alertId, "Acknowledged by " + userEmail);
        return mapToAlertDto(saved);
    }

    @Override
    public TransportAlertDto resolveAlert(Long alertId, String userEmail, String remarks, String schoolId) {
        TransportAlertEntity alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + alertId));

        alert.setStatus("RESOLVED");
        alert.setResolvedAt(LocalDateTime.now());
        if (remarks != null && !remarks.isBlank()) {
            alert.setResolutionRemarks(remarks);
        }

        TransportAlertEntity saved = alertRepository.save(alert);
        logAudit(alert.getSchoolId(), "ALERT_RESOLVED", "ALERT", alertId, "Resolved by " + userEmail);
        return mapToAlertDto(saved);
    }

    private TransportAlertDto mapToAlertDto(TransportAlertEntity a) {
        return TransportAlertDto.builder()
                .id(a.getId())
                .tripId(a.getTripId())
                .busId(a.getBusId())
                .busNumber(a.getBusNumber())
                .alertType(a.getAlertType())
                .severity(a.getSeverity())
                .title(a.getTitle())
                .message(a.getMessage())
                .status(a.getStatus())
                .acknowledgedBy(a.getAcknowledgedBy())
                .acknowledgedAt(a.getAcknowledgedAt())
                .resolvedAt(a.getResolvedAt())
                .resolutionRemarks(a.getResolutionRemarks())
                .createdAt(a.getCreatedAt())
                .build();
    }

    // ==========================================
    // PARENT & STUDENT ACCESS
    // ==========================================

    @Override
    public ParentTransportDto getParentChildTransport(String parentEmail, Long studentId) {
        StudentTransportAssignmentEntity assignment = studentAssignmentRepository.findFirstByStudentIdOrderByIdDesc(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("No transport assigned for student id: " + studentId));

        TransportRouteEntity route = routeRepository.findById(assignment.getRouteId()).orElse(null);
        RouteStopEntity stop = assignment.getPickupStopId() != null ? stopRepository.findById(assignment.getPickupStopId()).orElse(null) : null;

        // Find active trip for this route
        Optional<TransportTripEntity> activeTrip = tripRepository.findFirstByBusIdAndStatus(assignment.getRouteId(), "IN_PROGRESS");
        if (activeTrip.isEmpty()) {
            List<BusAssignmentEntity> busAssignments = assignmentRepository.findByRouteIdAndStatus(assignment.getRouteId(), "ACTIVE");
            if (!busAssignments.isEmpty()) {
                activeTrip = tripRepository.findFirstByBusIdAndStatus(busAssignments.get(0).getBusId(), "IN_PROGRESS");
            }
        }

        BusEntity bus = null;
        DriverProfileEntity driver = null;
        Double lat = 25.6020;
        Double lon = 85.1420;
        Double speed = 0.0;
        int eta = 8;
        String tripStatus = "SCHEDULED";
        LocalDateTime lastGps = LocalDateTime.now();

        if (activeTrip.isPresent()) {
            TransportTripEntity t = activeTrip.get();
            tripStatus = t.getStatus();
            lat = t.getLastLatitude();
            lon = t.getLastLongitude();
            speed = t.getCurrentSpeedKmh();
            eta = t.getNextStopEtaMinutes() != null ? t.getNextStopEtaMinutes() : 8;
            lastGps = t.getLastLocationTime();
            bus = busRepository.findById(t.getBusId()).orElse(null);
            driver = driverRepository.findById(t.getDriverId()).orElse(null);
        } else {
            List<BusAssignmentEntity> busAssignments = assignmentRepository.findByRouteIdAndStatus(assignment.getRouteId(), "ACTIVE");
            if (!busAssignments.isEmpty()) {
                bus = busRepository.findById(busAssignments.get(0).getBusId()).orElse(null);
                driver = driverRepository.findById(busAssignments.get(0).getDriverId()).orElse(null);
            }
        }

        return ParentTransportDto.builder()
                .studentId(studentId)
                .studentName(assignment.getStudentName())
                .admissionNo(assignment.getAdmissionNo())
                .className(assignment.getClassName())
                .routeId(assignment.getRouteId())
                .routeName(route != null ? route.getRouteName() : "Assigned Route")
                .stopName(stop != null ? stop.getStopName() : "Main Gate Stop")
                .scheduledPickupTime(assignment.getPickupTime() != null ? assignment.getPickupTime() : "07:30 AM")
                .scheduledDropTime(assignment.getDropTime() != null ? assignment.getDropTime() : "03:15 PM")
                .pickupStatus(assignment.getPickupStatus())
                .statusUpdatedAt(assignment.getLastStatusUpdate())
                .tripStatus(tripStatus)
                .busId(bus != null ? bus.getId() : null)
                .busNumber(bus != null ? bus.getBusNumber() : "Bus 04")
                .driverName(driver != null ? driver.getName() : "Ramesh Kumar")
                .driverPhone(driver != null ? driver.getPhone() : "+91 98765 43210")
                .busLatitude(lat)
                .busLongitude(lon)
                .busSpeedKmh(speed)
                .estimatedArrivalMinutes(eta)
                .currentStopName(stop != null ? stop.getStopName() : "Next Stop")
                .lastGpsUpdate(lastGps)
                .build();
    }

    @Override
    public StudentTransportDto getStudentTransport(String studentEmail) {
        List<StudentTransportAssignmentEntity> all = studentAssignmentRepository.findAll();
        StudentTransportAssignmentEntity assignment = all.isEmpty() ? null : all.get(0);

        if (assignment == null) {
            return StudentTransportDto.builder()
                    .studentName("Student")
                    .routeName("No route assigned")
                    .tripStatus("NONE")
                    .build();
        }

        TransportRouteEntity route = routeRepository.findById(assignment.getRouteId()).orElse(null);
        List<RouteStopDto> stops = stopRepository.findByRouteIdOrderByStopOrderAsc(assignment.getRouteId()).stream()
                .map(this::mapToRouteStopDto)
                .collect(Collectors.toList());

        String pickName = assignment.getPickupStopId() != null ? stopRepository.findById(assignment.getPickupStopId()).map(RouteStopEntity::getStopName).orElse("Stop 1") : "Main Stop";
        String dropName = assignment.getDropStopId() != null ? stopRepository.findById(assignment.getDropStopId()).map(RouteStopEntity::getStopName).orElse("Campus Gate") : "Campus Stop";

        return StudentTransportDto.builder()
                .studentId(assignment.getStudentId())
                .studentName(assignment.getStudentName())
                .admissionNo(assignment.getAdmissionNo())
                .className(assignment.getClassName())
                .routeId(assignment.getRouteId())
                .routeName(route != null ? route.getRouteName() : "Route 1")
                .pickupStopName(pickName)
                .pickupTime(assignment.getPickupTime() != null ? assignment.getPickupTime() : "07:30 AM")
                .dropStopName(dropName)
                .dropTime(assignment.getDropTime() != null ? assignment.getDropTime() : "03:15 PM")
                .busNumber("Bus 04")
                .driverName("Ramesh Kumar")
                .driverPhone("+91 98765 43210")
                .tripStatus("IN_PROGRESS")
                .estimatedArrivalMinutes(8)
                .routeStops(stops)
                .build();
    }

    // ==========================================
    // AUDIT LOGGING & HELPERS
    // ==========================================

    private void logAudit(String schoolId, String action, String resourceType, Long resourceId, String details) {
        try {
            TransportAuditLogEntity log = TransportAuditLogEntity.builder()
                    .schoolId(schoolId)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .result("SUCCESS")
                    .details(details)
                    .occurredAt(LocalDateTime.now())
                    .build();
            auditLogRepository.save(log);
        } catch (Exception ignored) {
        }
    }

    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // ==========================================
    // INITIAL DATA SEEDER
    // ==========================================

    @Override
    public synchronized void seedInitialTransportDataIfEmpty(String schoolId) {
        String sId = resolveSchool(schoolId);
        if (busRepository.countBySchoolId(sId) > 0 || busRepository.count() > 0 || busRepository.findByRegistrationNumberIgnoreCase("MH-02-AB-1001").isPresent()) {
            return;
        }

        log.info("Seeding initial rich transport data for school {}", sId);

        // 1. Seed Buses
        BusEntity bus1 = BusEntity.builder()
                .schoolId(sId)
                .busNumber("Bus 01")
                .registrationNumber("MH-02-AB-1001")
                .capacity(42)
                .model("Starbus Ultra")
                .manufacturer("Tata Motors")
                .fuelType("CNG")
                .gpsDeviceId("GPS-TT-1001")
                .insuranceExpiryDate(LocalDate.now().plusMonths(9))
                .fitnessExpiryDate(LocalDate.now().plusMonths(11))
                .pollutionExpiryDate(LocalDate.now().plusMonths(5))
                .permitExpiryDate(LocalDate.now().plusMonths(14))
                .status("ACTIVE")
                .currentSpeed(0.0)
                .lastLatitude(25.6120)
                .lastLongitude(85.1320)
                .lastGpsUpdate(LocalDateTime.now().minusMinutes(2))
                .build();

        BusEntity bus2 = BusEntity.builder()
                .schoolId(sId)
                .busNumber("Bus 02")
                .registrationNumber("MH-02-CD-2002")
                .capacity(38)
                .model("Cruiser Deluxe")
                .manufacturer("Force Motors")
                .fuelType("DIESEL")
                .gpsDeviceId("GPS-FC-2002")
                .insuranceExpiryDate(LocalDate.now().plusMonths(6))
                .fitnessExpiryDate(LocalDate.now().plusMonths(8))
                .pollutionExpiryDate(LocalDate.now().plusMonths(3))
                .permitExpiryDate(LocalDate.now().plusMonths(12))
                .status("ACTIVE")
                .currentSpeed(0.0)
                .lastLatitude(25.5940)
                .lastLongitude(85.1620)
                .lastGpsUpdate(LocalDateTime.now().minusMinutes(5))
                .build();

        BusEntity bus4 = BusEntity.builder()
                .schoolId(sId)
                .busNumber("Bus 04")
                .registrationNumber("MH-02-EF-4004")
                .capacity(45)
                .model("Skyline Pro")
                .manufacturer("Eicher")
                .fuelType("DIESEL")
                .gpsDeviceId("GPS-EC-4004")
                .insuranceExpiryDate(LocalDate.now().plusMonths(10))
                .fitnessExpiryDate(LocalDate.now().plusMonths(12))
                .pollutionExpiryDate(LocalDate.now().plusMonths(6))
                .permitExpiryDate(LocalDate.now().plusMonths(18))
                .status("ACTIVE")
                .currentSpeed(32.5)
                .lastLatitude(25.6025)
                .lastLongitude(85.1430)
                .lastGpsUpdate(LocalDateTime.now().minusSeconds(15))
                .build();

        try {
            busRepository.saveAll(List.of(bus1, bus2, bus4));
        } catch (Exception e) {
            log.warn("Notice saving initial buses: {}", e.getMessage());
            return;
        }

        // 2. Seed Drivers & Driver Users (NO OTP: standard email & password)
        UserEntity driverUser = userRepository.findByEmailIgnoreCase("driver@school.com").orElseGet(() -> {
            UserEntity u = UserEntity.builder()
                    .email("driver@school.com")
                    .name("Ramesh Kumar")
                    .password(passwordEncoder.encode("password"))
                    .role(Role.DRIVER)
                    .enabled(true)
                    .build();
            return userRepository.save(u);
        });

        DriverProfileEntity driver1 = DriverProfileEntity.builder()
                .schoolId(sId)
                .userId(driverUser.getId())
                .name("Ramesh Kumar")
                .email("driver@school.com")
                .phone("+91 98765 43210")
                .licenseNumber("DL-0420110056789")
                .licenseExpiryDate(LocalDate.now().plusYears(3))
                .licenseCategory("COMMERCIAL_HEAVY")
                .experienceYears(8)
                .joiningDate(LocalDate.now().minusYears(3))
                .verificationStatus("VERIFIED")
                .status("ACTIVE")
                .avatarUrl("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=150&q=80")
                .emergencyContact("+91 98765 00000 (Spouse)")
                .address("Sector 4, Green Avenue, City")
                .assignedBusId(bus4.getId())
                .build();

        DriverProfileEntity driver2 = DriverProfileEntity.builder()
                .schoolId(sId)
                .name("Suresh Yadav")
                .email("suresh.driver@school.com")
                .phone("+91 98123 45678")
                .licenseNumber("DL-0420150012345")
                .licenseExpiryDate(LocalDate.now().plusYears(2))
                .licenseCategory("COMMERCIAL_HEAVY")
                .experienceYears(6)
                .joiningDate(LocalDate.now().minusYears(2))
                .verificationStatus("VERIFIED")
                .status("ACTIVE")
                .avatarUrl("https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=150&q=80")
                .emergencyContact("+91 98123 99999")
                .address("Block B, Shastri Nagar, City")
                .assignedBusId(bus1.getId())
                .build();

        driverRepository.saveAll(List.of(driver1, driver2));

        // 3. Seed Routes & Stops
        TransportRouteEntity route1 = TransportRouteEntity.builder()
                .schoolId(sId)
                .routeName("Route 1 - North Campus Loop")
                .routeCode("RT-NORTH-01")
                .description("Serves North City, City Center, Metro Station, and School North Gate")
                .startLocation("North City Square")
                .endLocation("School Main Gate")
                .areaLocality("North City & Bailey Road")
                .fare(1800.0)
                .plannedDurationMinutes(45)
                .totalDistanceKm(18.5)
                .morningStartTime("07:15 AM")
                .eveningStartTime("02:45 PM")
                .speedLimitKmh(45)
                .status("ACTIVE")
                .build();

        TransportRouteEntity route2 = TransportRouteEntity.builder()
                .schoolId(sId)
                .routeName("Route 2 - South Residential Belt")
                .routeCode("RT-SOUTH-02")
                .description("Covers Southern Apartments, Central Market, and Green Park")
                .startLocation("Green Park Gate 1")
                .endLocation("School Main Gate")
                .areaLocality("Kankarbagh & South Residency")
                .fare(2200.0)
                .plannedDurationMinutes(50)
                .totalDistanceKm(22.0)
                .morningStartTime("07:05 AM")
                .eveningStartTime("02:40 PM")
                .speedLimitKmh(50)
                .status("ACTIVE")
                .build();

        routeRepository.saveAll(List.of(route1, route2));

        List<RouteStopEntity> r1Stops = List.of(
                RouteStopEntity.builder().schoolId(sId).routeId(route1.getId()).stopName("North City Square").stopOrder(1).latitude(25.6150).longitude(85.1280).plannedMorningPickupTime("07:15 AM").plannedEveningDropTime("03:30 PM").geofenceRadiusMeters(60).landmark("Opposite HDFC Bank").assignedStudentsCount(8).build(),
                RouteStopEntity.builder().schoolId(sId).routeId(route1.getId()).stopName("Metro Station Pillar 42").stopOrder(2).latitude(25.6100).longitude(85.1350).plannedMorningPickupTime("07:25 AM").plannedEveningDropTime("03:20 PM").geofenceRadiusMeters(50).landmark("Near Gate 2 Escalator").assignedStudentsCount(12).build(),
                RouteStopEntity.builder().schoolId(sId).routeId(route1.getId()).stopName("Central Mall Junction").stopOrder(3).latitude(25.6050).longitude(85.1410).plannedMorningPickupTime("07:35 AM").plannedEveningDropTime("03:10 PM").geofenceRadiusMeters(50).landmark("Bus Bay 3").assignedStudentsCount(10).build(),
                RouteStopEntity.builder().schoolId(sId).routeId(route1.getId()).stopName("Kankarbagh Park").stopOrder(4).latitude(25.6010).longitude(85.1480).plannedMorningPickupTime("07:45 AM").plannedEveningDropTime("03:00 PM").geofenceRadiusMeters(50).landmark("Roundabout Park Corner").assignedStudentsCount(9).build(),
                RouteStopEntity.builder().schoolId(sId).routeId(route1.getId()).stopName("School Main Gate (Arrival)").stopOrder(5).latitude(25.5970).longitude(85.1550).plannedMorningPickupTime("08:00 AM").plannedEveningDropTime("02:45 PM").geofenceRadiusMeters(70).landmark("Campus Bus Bay").assignedStudentsCount(39).build()
        );
        stopRepository.saveAll(r1Stops);

        // 4. Seed Bus Assignments
        BusAssignmentEntity assignment1 = BusAssignmentEntity.builder()
                .schoolId(sId)
                .driverId(driver1.getId())
                .busId(bus4.getId())
                .routeId(route1.getId())
                .shift("BOTH")
                .effectiveStartDate(LocalDate.now().minusMonths(1))
                .status("ACTIVE")
                .notes("Primary daily route assignment")
                .build();

        BusAssignmentEntity assignment2 = BusAssignmentEntity.builder()
                .schoolId(sId)
                .driverId(driver2.getId())
                .busId(bus1.getId())
                .routeId(route2.getId())
                .shift("BOTH")
                .effectiveStartDate(LocalDate.now().minusMonths(1))
                .status("ACTIVE")
                .notes("Secondary feeder route")
                .build();

        assignmentRepository.saveAll(List.of(assignment1, assignment2));

        // 5. Seed Student Assignments
        StudentTransportAssignmentEntity sa1 = StudentTransportAssignmentEntity.builder()
                .schoolId(sId)
                .studentId(1L)
                .studentName("Aarav Sharma")
                .admissionNo("ADM-2024-001")
                .className("Class 9-A")
                .routeId(route1.getId())
                .pickupStopId(r1Stops.get(1).getId())
                .dropStopId(r1Stops.get(1).getId())
                .pickupTime("07:25 AM")
                .dropTime("03:20 PM")
                .guardianPhone("+91 98765 11111")
                .tripType("BOTH_WAY")
                .startDate(LocalDate.now().minusMonths(2))
                .monthlyFee(1800.0)
                .feeStatus("PAID")
                .status("ACTIVE")
                .pickupStatus("PICKED_UP")
                .build();

        StudentTransportAssignmentEntity sa2 = StudentTransportAssignmentEntity.builder()
                .schoolId(sId)
                .studentId(2L)
                .studentName("Ananya Verma")
                .admissionNo("ADM-2024-002")
                .className("Class 7-B")
                .routeId(route1.getId())
                .pickupStopId(r1Stops.get(2).getId())
                .dropStopId(r1Stops.get(2).getId())
                .pickupTime("07:35 AM")
                .dropTime("03:10 PM")
                .guardianPhone("+91 98765 22222")
                .tripType("BOTH_WAY")
                .startDate(LocalDate.now().minusMonths(2))
                .monthlyFee(1800.0)
                .feeStatus("PENDING")
                .status("ACTIVE")
                .pickupStatus("SCHEDULED")
                .build();

        studentAssignmentRepository.saveAll(List.of(sa1, sa2));

        // Seed a demo fee payment for sa1
        TransportFeePaymentEntity demoPayment = TransportFeePaymentEntity.builder()
                .schoolId(sId)
                .assignmentId(sa1.getId())
                .studentId(sa1.getStudentId())
                .studentName(sa1.getStudentName())
                .admissionNo(sa1.getAdmissionNo())
                .receiptNumber("TR-REC-2026-001")
                .amountPaid(1800.0)
                .paymentDate(LocalDate.now().minusDays(5))
                .paymentMode("UPI")
                .feeMonth(LocalDate.now().getMonth().name())
                .remarks("Online Fee Portal payment")
                .receivedBy("Accounts Dept")
                .build();
        feePaymentRepository.save(demoPayment);

        // 6. Seed Active Demo Trip for Bus 04 with GPS points
        TransportTripEntity activeTrip = TransportTripEntity.builder()
                .schoolId(sId)
                .busAssignmentId(assignment1.getId())
                .driverId(driver1.getId())
                .busId(bus4.getId())
                .routeId(route1.getId())
                .shift("MORNING")
                .tripDate(LocalDate.now())
                .scheduledStartTime(LocalDateTime.now().minusMinutes(25))
                .actualStartTime(LocalDateTime.now().minusMinutes(22))
                .status("IN_PROGRESS")
                .totalDistanceKm(7.8)
                .currentSpeedKmh(34.0)
                .lastLatitude(25.6025)
                .lastLongitude(85.1430)
                .lastLocationTime(LocalDateTime.now().minusSeconds(10))
                .currentStopId(r1Stops.get(2).getId())
                .nextStopId(r1Stops.get(3).getId())
                .nextStopEtaMinutes(6)
                .completedStopsCount(2)
                .totalStopsCount(5)
                .preTripCheckPassed(true)
                .preTripNotes("Tires, brakes, indicators and emergency kit checked.")
                .build();

        TransportTripEntity savedTrip = tripRepository.save(activeTrip);

        // Seed Stop Events for completed stops
        TripStopEventEntity se1 = TripStopEventEntity.builder()
                .schoolId(sId)
                .tripId(savedTrip.getId())
                .stopId(r1Stops.get(0).getId())
                .stopName(r1Stops.get(0).getStopName())
                .stopOrder(1)
                .arrivalTime(LocalDateTime.now().minusMinutes(20))
                .departureTime(LocalDateTime.now().minusMinutes(18))
                .dwellDurationSeconds(120L)
                .status("DEPARTED")
                .studentsBoardedCount(8)
                .studentsDeboardedCount(0)
                .build();

        TripStopEventEntity se2 = TripStopEventEntity.builder()
                .schoolId(sId)
                .tripId(savedTrip.getId())
                .stopId(r1Stops.get(1).getId())
                .stopName(r1Stops.get(1).getStopName())
                .stopOrder(2)
                .arrivalTime(LocalDateTime.now().minusMinutes(12))
                .departureTime(LocalDateTime.now().minusMinutes(10))
                .dwellDurationSeconds(130L)
                .status("DEPARTED")
                .studentsBoardedCount(12)
                .studentsDeboardedCount(0)
                .build();

        stopEventRepository.saveAll(List.of(se1, se2));

        // Seed GPS trail for playback
        List<BusLocationHistoryEntity> gpsTrail = List.of(
                BusLocationHistoryEntity.builder().schoolId(sId).tripId(savedTrip.getId()).busId(bus4.getId()).driverId(driver1.getId()).latitude(25.6150).longitude(85.1280).speedKmh(0.0).heading(0.0).recordedAt(LocalDateTime.now().minusMinutes(22)).serverReceivedAt(LocalDateTime.now().minusMinutes(22)).build(),
                BusLocationHistoryEntity.builder().schoolId(sId).tripId(savedTrip.getId()).busId(bus4.getId()).driverId(driver1.getId()).latitude(25.6135).longitude(85.1305).speedKmh(28.0).heading(135.0).recordedAt(LocalDateTime.now().minusMinutes(19)).serverReceivedAt(LocalDateTime.now().minusMinutes(19)).build(),
                BusLocationHistoryEntity.builder().schoolId(sId).tripId(savedTrip.getId()).busId(bus4.getId()).driverId(driver1.getId()).latitude(25.6100).longitude(85.1350).speedKmh(0.0).heading(140.0).recordedAt(LocalDateTime.now().minusMinutes(12)).serverReceivedAt(LocalDateTime.now().minusMinutes(12)).build(),
                BusLocationHistoryEntity.builder().schoolId(sId).tripId(savedTrip.getId()).busId(bus4.getId()).driverId(driver1.getId()).latitude(25.6075).longitude(85.1380).speedKmh(35.0).heading(145.0).recordedAt(LocalDateTime.now().minusMinutes(6)).serverReceivedAt(LocalDateTime.now().minusMinutes(6)).build(),
                BusLocationHistoryEntity.builder().schoolId(sId).tripId(savedTrip.getId()).busId(bus4.getId()).driverId(driver1.getId()).latitude(25.6025).longitude(85.1430).speedKmh(34.0).heading(150.0).recordedAt(LocalDateTime.now().minusSeconds(10)).serverReceivedAt(LocalDateTime.now().minusSeconds(10)).build()
        );
        locationHistoryRepository.saveAll(gpsTrail);

        // 7. Seed Initial Fleet Alert
        TransportAlertEntity demoAlert = TransportAlertEntity.builder()
                .schoolId(sId)
                .tripId(savedTrip.getId())
                .busId(bus4.getId())
                .busNumber("Bus 04")
                .alertType("DELAY")
                .severity("INFO")
                .title("Traffic Congestion on Metro Corridor")
                .message("Slight 4-minute delay due to peak hour traffic near Metro Station Pillar 42.")
                .status("OPEN")
                .build();
        alertRepository.save(demoAlert);

        log.info("Finished seeding initial transport data successfully.");
    }
}

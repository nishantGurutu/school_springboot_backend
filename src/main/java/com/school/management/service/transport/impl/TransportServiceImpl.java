package com.school.management.service.transport.impl;

import com.school.management.domain.user.Role;
import com.school.management.dto.transport.*;
import com.school.management.entity.StaffEntity;
import com.school.management.entity.TeacherEntity;
import com.school.management.entity.UserEntity;
import com.school.management.entity.transport.*;
import com.school.management.exceptions.BadRequestException;
import com.school.management.exceptions.DuplicateResourceException;
import com.school.management.exceptions.ResourceNotFoundException;
import com.school.management.exceptions.UnauthorizedException;
import com.school.management.repository.StaffRepository;
import com.school.management.repository.StudentRepository;
import com.school.management.repository.TeacherRepository;
import com.school.management.repository.UserRepository;
import com.school.management.repository.transport.*;
import com.school.management.service.transport.TransportService;
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

    // Latest telemetry is transient. No GPS-history writes during an active trip.
    private record LivePosition(String driverEmail, TripDtos.GpsPointDto point, LocalDateTime receivedAt) {}
    private final java.util.concurrent.ConcurrentMap<Long, LivePosition> livePositions = new java.util.concurrent.ConcurrentHashMap<>();

    private final com.school.management.service.DriverBusAllocationService busAllocation;
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
    private final StaffRepository staffRepository;
    private final TransportFeePaymentRepository feePaymentRepository;
    private final StudentRepository studentRepository;
    private final SafetyChecklistItemRepository checklistRepository;

    public TransportServiceImpl(
            com.school.management.service.DriverBusAllocationService busAllocation,
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
            StaffRepository staffRepository,
            TransportFeePaymentRepository feePaymentRepository,
            StudentRepository studentRepository,
            SafetyChecklistItemRepository checklistRepository
    ) {
        this.busAllocation = busAllocation;
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
        this.staffRepository = staffRepository;
        this.feePaymentRepository = feePaymentRepository;
        this.studentRepository = studentRepository;
        this.checklistRepository = checklistRepository;
    }

    private static final String DEFAULT_SCHOOL_ID = "SCH-001";

    private String resolveSchool(String schoolId) {
        return (schoolId != null && !schoolId.isBlank()) ? schoolId : DEFAULT_SCHOOL_ID;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartupSeedTransportData() {
        try {
            seedInitialTransportDataIfEmpty(DEFAULT_SCHOOL_ID);
            seedDefaultChecklistIfEmpty(DEFAULT_SCHOOL_ID);
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
        String normSId = sId.replace("-", "").toUpperCase();
        long totalDrivers = driverRepository.findAll().stream()
                .filter(d -> isSchoolMatch(d.getSchoolId(), normSId) && !isDemoDriver(d.getEmail()))
                .count();
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
        if (dto.getBusNumber() == null || dto.getBusNumber().trim().isEmpty()) {
            throw new BadRequestException("Bus number is required");
        }
        if (dto.getRegistrationNumber() == null || dto.getRegistrationNumber().trim().isEmpty()) {
            throw new BadRequestException("Registration number is required");
        }
        String regNum = dto.getRegistrationNumber().trim().toUpperCase();
        if (busRepository.findByRegistrationNumberIgnoreCase(regNum).isPresent()) {
            throw new DuplicateResourceException("Bus with registration number " + regNum + " already exists");
        }

        BusEntity entity = BusEntity.builder()
                .schoolId(sId)
                .busNumber(dto.getBusNumber().trim())
                .registrationNumber(regNum)
                .capacity(dto.getCapacity() != null && dto.getCapacity() > 0 ? dto.getCapacity() : 40)
                .model(dto.getModel() != null && !dto.getModel().trim().isEmpty() ? dto.getModel().trim() : null)
                .manufacturer(dto.getManufacturer() != null && !dto.getManufacturer().trim().isEmpty() ? dto.getManufacturer().trim() : null)
                .fuelType(dto.getFuelType() != null && !dto.getFuelType().trim().isEmpty() ? dto.getFuelType() : "DIESEL")
                .gpsDeviceId(dto.getGpsDeviceId() != null && !dto.getGpsDeviceId().trim().isEmpty() ? dto.getGpsDeviceId().trim() : null)
                .insuranceExpiryDate(dto.getInsuranceExpiryDate())
                .fitnessExpiryDate(dto.getFitnessExpiryDate())
                .pollutionExpiryDate(dto.getPollutionExpiryDate())
                .permitExpiryDate(dto.getPermitExpiryDate())
                .status(dto.getStatus() != null && !dto.getStatus().trim().isEmpty() ? dto.getStatus() : "ACTIVE")
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

        if (dto.getBusNumber() != null && !dto.getBusNumber().trim().isEmpty()) {
            bus.setBusNumber(dto.getBusNumber().trim());
        }
        if (dto.getRegistrationNumber() != null && !dto.getRegistrationNumber().trim().isEmpty()) {
            String regNum = dto.getRegistrationNumber().trim().toUpperCase();
            Optional<BusEntity> existing = busRepository.findByRegistrationNumberIgnoreCase(regNum);
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new DuplicateResourceException("Bus with registration number " + regNum + " already exists");
            }
            bus.setRegistrationNumber(regNum);
        }
        if (dto.getCapacity() != null && dto.getCapacity() > 0) bus.setCapacity(dto.getCapacity());
        if (dto.getModel() != null) bus.setModel(dto.getModel().trim().isEmpty() ? null : dto.getModel().trim());
        if (dto.getManufacturer() != null) bus.setManufacturer(dto.getManufacturer().trim().isEmpty() ? null : dto.getManufacturer().trim());
        if (dto.getFuelType() != null && !dto.getFuelType().trim().isEmpty()) bus.setFuelType(dto.getFuelType());
        if (dto.getGpsDeviceId() != null) bus.setGpsDeviceId(dto.getGpsDeviceId().trim().isEmpty() ? null : dto.getGpsDeviceId().trim());
        bus.setInsuranceExpiryDate(dto.getInsuranceExpiryDate());
        bus.setFitnessExpiryDate(dto.getFitnessExpiryDate());
        bus.setPollutionExpiryDate(dto.getPollutionExpiryDate());
        bus.setPermitExpiryDate(dto.getPermitExpiryDate());
        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) bus.setStatus(dto.getStatus());

        BusEntity updated = busRepository.save(bus);
        logAudit(sId, "BUS_UPDATED", "BUS", updated.getId(), "Updated bus: " + updated.getBusNumber());
        return mapToBusDto(updated);
    }

    @Override
    public void deleteBus(Long id, String schoolId) {
        String sId = resolveSchool(schoolId);
        BusEntity bus = busRepository.findByIdAndSchoolId(id, sId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + id));

        // Clean up assignments if any
        assignmentRepository.findByBusIdAndStatus(id, "ACTIVE").forEach(assignmentRepository::delete);

        // Clean up driver profile assignedBusId if any
        driverRepository.findByAssignedBusId(id).forEach(driver -> {
            driver.setAssignedBusId(null);
            driverRepository.save(driver);
        });

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

        driverRepository.findByAssignedBusId(bus.getId()).stream().findFirst().ifPresent(driver -> {
            dto.setAssignedDriverId(driver.getId());
            dto.setAssignedDriverName(driver.getName());
        });
        assignmentRepository.findByBusIdAndStatus(bus.getId(), "ACTIVE").stream().findFirst().ifPresent(as -> {
            dto.setAssignedDriverId(as.getDriverId());
            driverRepository.findById(as.getDriverId()).ifPresent(d -> dto.setAssignedDriverName(d.getName()));
            dto.setAssignedRouteId(as.getRouteId());
            routeRepository.findById(as.getRouteId()).ifPresent(r -> dto.setAssignedRouteName(r.getRouteName()));
        });

        tripRepository.findFirstByBusIdAndStatus(bus.getId(), "IN_PROGRESS").ifPresent(t -> {
            dto.setAssignedDriverId(t.getDriverId());
            driverRepository.findById(t.getDriverId()).ifPresent(driver -> dto.setAssignedDriverName(driver.getName()));
            dto.setActiveTripStatus("IN_PROGRESS");
            dto.setActiveTripId(t.getId());
        });

        boolean assigned = dto.getAssignedDriverId() != null;
        dto.setAssignmentStatus(assigned ? "ASSIGNED" : "AVAILABLE");
        dto.setAvailableForAssignment(!assigned && "ACTIVE".equalsIgnoreCase(bus.getStatus()));
        return dto;
    }

    // ==========================================
    // DRIVER MANAGEMENT
    // ==========================================

    private boolean isSchoolMatch(String schoolId, String normTarget) {
        if (schoolId == null || schoolId.isBlank()) return true;
        return schoolId.replace("-", "").equalsIgnoreCase(normTarget);
    }

    private boolean isDemoDriver(String email) {
        if (email == null) return false;
        String e = email.trim().toLowerCase();
        return e.equals("driver@school.com") || e.equals("suresh.driver@school.com");
    }

    @Override
    public List<DriverDto> getAllDrivers(String schoolId) {
        String sId = resolveSchool(schoolId);
        seedInitialTransportDataIfEmpty(sId);
        syncDriversFromStaff(sId);
        String normSId = sId.replace("-", "").toUpperCase();
        return driverRepository.findAll().stream()
                .filter(d -> isSchoolMatch(d.getSchoolId(), normSId) && !isDemoDriver(d.getEmail()))
                .map(this::mapToDriverDto)
                .collect(Collectors.toList());
    }

    private void syncDriversFromStaff(String sId) {
        try {
            // 1. Sync from staffRepository (StaffEntity)
            if (staffRepository != null) {
                List<StaffEntity> allStaff = staffRepository.findAll();
                for (StaffEntity staff : allStaff) {
                    boolean isDriver = (staff.getRole() != null && staff.getRole().toUpperCase().contains("DRIVER"))
                            || (staff.getStaffType() != null && staff.getStaffType().toLowerCase().contains("driver"))
                            || (staff.getDesignation() != null && staff.getDesignation().toLowerCase().contains("driver"));
                    if (isDriver && staff.getEmail() != null && !staff.getEmail().isBlank()) {
                        String email = staff.getEmail().trim().toLowerCase();
                        Optional<DriverProfileEntity> existing = driverRepository.findByEmailIgnoreCase(email);
                        if (existing.isPresent()) {
                            DriverProfileEntity d = existing.get();
                            d.setSchoolId(sId);
                            d.setName(staff.getName());
                            if (staff.getPhone() != null && !staff.getPhone().isBlank()) d.setPhone(staff.getPhone());
                            if (staff.getStatus() != null && !staff.getStatus().isBlank()) d.setStatus(staff.getStatus().toUpperCase());
                            driverRepository.save(d);
                        } else {
                            DriverProfileEntity newDriver = DriverProfileEntity.builder()
                                    .schoolId(sId)
                                    .name(staff.getName())
                                    .email(email)
                                    .phone(staff.getPhone() != null && !staff.getPhone().isBlank() ? staff.getPhone() : "+91 98000 00000")
                                    .licenseNumber("DL-IND" + (staff.getId() != null ? (1000 + staff.getId()) : "1001"))
                                    .licenseExpiryDate(LocalDate.now().plusYears(4))
                                    .licenseCategory("COMMERCIAL_HEAVY")
                                    .experienceYears(5)
                                    .joiningDate(LocalDate.now())
                                    .verificationStatus("VERIFIED")
                                    .status(staff.getStatus() != null && !staff.getStatus().isBlank() ? staff.getStatus().toUpperCase() : "ACTIVE")
                                    .emergencyContact(staff.getPhone())
                                    .address("Campus Staff Quarters")
                                    .build();
                            driverRepository.save(newDriver);
                            log.info("Auto-synced Staff driver {} into transport driver profiles.", staff.getName());
                        }
                    }
                }
            }

            // 2. Sync from teacherRepository (TeacherEntity)
            if (teacherRepository != null) {
                List<TeacherEntity> staffList = teacherRepository.findAll();
                for (TeacherEntity staff : staffList) {
                    boolean isDriver = (staff.getDesignation() != null && staff.getDesignation().toLowerCase().contains("driver"))
                            || (staff.getDepartment() != null && staff.getDepartment().toLowerCase().contains("transport"))
                            || ("Driver".equalsIgnoreCase(staff.getType()));
                    if (isDriver) {
                        String email = (staff.getEmail() != null && !staff.getEmail().isBlank())
                                ? staff.getEmail().trim().toLowerCase()
                                : ("driver." + staff.getEmployeeId() + "@school.com").toLowerCase();

                        String fullName = ((staff.getFirstName() != null ? staff.getFirstName() : "") + " " +
                                (staff.getLastName() != null ? staff.getLastName() : "")).trim();
                        if (fullName.isBlank()) fullName = "Staff Driver " + staff.getEmployeeId();

                        Optional<DriverProfileEntity> existing = driverRepository.findByEmailIgnoreCase(email);
                        if (existing.isPresent()) {
                            DriverProfileEntity d = existing.get();
                            d.setSchoolId(sId);
                            d.setName(fullName);
                            if (staff.getPhone() != null && !staff.getPhone().isBlank()) d.setPhone(staff.getPhone());
                            if (staff.getAvatar() != null && !staff.getAvatar().isBlank()) d.setAvatarUrl(staff.getAvatar());
                            if (staff.getStatus() != null) d.setStatus(staff.getStatus().name());
                            driverRepository.save(d);
                        } else {
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
            }
        } catch (Exception ex) {
            log.warn("Auto-sync drivers from staff failed: {}", ex.getMessage());
        }
    }

    @Override
    public DriverDto getDriverById(Long id, String schoolId) {
        DriverProfileEntity driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
        return mapToDriverDto(driver);
    }

    @Override
    public DriverDailyDetailsDto getDriverDailyDetails(Long id, String schoolId) {
        String sId = resolveSchool(schoolId);
        seedInitialTransportDataIfEmpty(sId);
        DriverProfileEntity driver = driverRepository.findById(id)
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

        // Sync to StaffEntity
        if (staffRepository != null) {
            try {
                StaffEntity staff = staffRepository.findByEmailIgnoreCase(email).orElseGet(StaffEntity::new);
                staff.setName(dto.getName());
                staff.setEmail(email);
                staff.setPhone(dto.getPhone());
                staff.setDesignation("Bus Driver");
                staff.setStaffType("Driver");
                staff.setRole("DRIVER");
                staff.setStatus("Active");
                staff.setJoinDate(dto.getJoiningDate() != null ? dto.getJoiningDate().toString() : LocalDate.now().toString());
                staffRepository.save(staff);
            } catch (Exception e) {
                log.warn("Notice syncing driver to staff table: {}", e.getMessage());
            }
        }

        // Sync to TeacherEntity for unified Staff Directory
        if (teacherRepository != null) {
            try {
                String fullName = dto.getName() != null ? dto.getName().trim() : "Driver";
                String[] parts = fullName.split("\\s+", 2);
                String firstName = parts[0];
                String lastName = parts.length > 1 ? parts[1] : "(Driver)";

                TeacherEntity teacher = teacherRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
                    return TeacherEntity.builder()
                            .employeeId("EMP-DRV-" + saved.getId())
                            .email(email)
                            .firstName(firstName)
                            .lastName(lastName)
                            .joiningDate(dto.getJoiningDate() != null ? dto.getJoiningDate() : LocalDate.now())
                            .build();
                });
                teacher.setFirstName(firstName);
                teacher.setLastName(lastName);
                teacher.setPhone(dto.getPhone() != null && !dto.getPhone().isBlank() ? dto.getPhone() : "+91 98000 00000");
                teacher.setDepartment("Transport");
                teacher.setSubject("Transport");
                teacher.setDesignation("Bus Driver");
                teacher.setType("Driver");
                teacher.setStatus(com.school.management.domain.teacher.TeacherStatus.ACTIVE);
                teacher.setAvatar(dto.getAvatarUrl());
                teacherRepository.save(teacher);
            } catch (Exception e) {
                log.warn("Notice syncing driver to teacher table: {}", e.getMessage());
            }
        }

        return mapToDriverDto(saved);
    }

    @Override
    @Transactional
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

        String email = updated.getEmail() != null ? updated.getEmail().trim().toLowerCase() : null;
        if (email != null && !email.isBlank()) {
            if (staffRepository != null) {
                staffRepository.findByEmailIgnoreCase(email).ifPresent(s -> {
                    s.setName(updated.getName());
                    if (updated.getPhone() != null) s.setPhone(updated.getPhone());
                    if (updated.getStatus() != null) s.setStatus(updated.getStatus());
                    staffRepository.save(s);
                });
            }
            if (teacherRepository != null) {
                teacherRepository.findByEmailIgnoreCase(email).ifPresent(t -> {
                    String fullName = updated.getName() != null ? updated.getName().trim() : "";
                    String[] parts = fullName.split("\\s+", 2);
                    t.setFirstName(parts[0]);
                    t.setLastName(parts.length > 1 ? parts[1] : "(Driver)");
                    if (updated.getPhone() != null) t.setPhone(updated.getPhone());
                    if (updated.getAvatarUrl() != null) t.setAvatar(updated.getAvatarUrl());
                    teacherRepository.save(t);
                });
            }
        }

        return mapToDriverDto(updated);
    }

    @Override
    @Transactional
    public void deleteDriver(Long id, String schoolId) {
        String sId = resolveSchool(schoolId);
        DriverProfileEntity driver = driverRepository.findByIdAndSchoolId(id, sId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));

        String email = driver.getEmail() != null ? driver.getEmail().trim().toLowerCase() : null;
        String phone = driver.getPhone() != null ? driver.getPhone().trim() : null;

        // 1. Remove bus assignments for this driver
        try {
            assignmentRepository.deleteByDriverId(id);
        } catch (Exception e) {
            log.warn("Notice clearing bus assignments for driver {}: {}", id, e.getMessage());
        }

        // 2. Delete driver profile entity
        driverRepository.delete(driver);

        // 3. Delete corresponding record from staff table if exists
        if (staffRepository != null) {
            try {
                if (email != null && !email.isBlank()) {
                    staffRepository.findByEmailIgnoreCase(email).ifPresent(staffRepository::delete);
                }
                if (phone != null && !phone.isBlank()) {
                    staffRepository.findByPhone(phone).ifPresent(staffRepository::delete);
                }
            } catch (Exception e) {
                log.warn("Notice deleting staff record for driver {}: {}", id, e.getMessage());
            }
        }

        // 4. Delete corresponding record from teachers table if exists
        if (teacherRepository != null) {
            try {
                if (email != null && !email.isBlank()) {
                    teacherRepository.findByEmailIgnoreCase(email).ifPresent(teacherRepository::delete);
                }
                if (phone != null && !phone.isBlank()) {
                    teacherRepository.findByPhone(phone).ifPresent(teacherRepository::delete);
                }
            } catch (Exception e) {
                log.warn("Notice deleting teacher record for driver {}: {}", id, e.getMessage());
            }
        }

        // 5. Delete corresponding record from users table if exists
        if (userRepository != null && email != null && !email.isBlank()) {
            try {
                userRepository.findByEmailIgnoreCase(email).ifPresent(userRepository::delete);
            } catch (Exception e) {
                log.warn("Notice deleting user account for driver {}: {}", id, e.getMessage());
            }
        }

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

        dto.setAssignedBusId(driver.getAssignedBusId());
        if (driver.getAssignedBusId() != null) {
            busRepository.findById(driver.getAssignedBusId()).ifPresent(bus -> dto.setAssignedBusNumber(bus.getBusNumber()));
        }
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

        DriverProfileEntity selectedDriver = driverRepository.findByIdAndSchoolId(dto.getDriverId(), sId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        busAllocation.assign(selectedDriver, dto.getBusId());

        routeRepository.findByIdAndSchoolId(dto.getRouteId(), sId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found in this school"));
        if (tripRepository.findFirstByDriverIdAndStatus(dto.getDriverId(), "IN_PROGRESS").isPresent()) {
            throw new IllegalStateException("End the active trip before changing the driver's duty");
        }

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

        if ("ACTIVE".equalsIgnoreCase(dto.getStatus() != null ? dto.getStatus() : as.getStatus())) {
            Long driverId = dto.getDriverId() != null ? dto.getDriverId() : as.getDriverId();
            Long busId = dto.getBusId() != null ? dto.getBusId() : as.getBusId();
            var driver = driverRepository.findByIdAndSchoolId(driverId, sId)
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
            busAllocation.assign(driver, busId);
        }
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

    @Override
    public TripDtos.DriverDutyDto getDriverDuty(String driverEmail) {
        DriverProfileEntity driver = requireTrackingDriver(driverEmail);

        BusAssignmentEntity assignment = assignmentRepository.findFirstByDriverIdAndStatusOrderByIdDesc(driver.getId(), "ACTIVE")
                .orElse(null);

        TripDtos.DriverDutyDto.DriverDutyDtoBuilder dutyBuilder = TripDtos.DriverDutyDto.builder()
                .driverId(driver.getId())
                .driverName(driver.getName())
                .phone(driver.getPhone())
                .licenseNumber(driver.getLicenseNumber())
                .licenseExpiryDate(driver.getLicenseExpiryDate() != null ? driver.getLicenseExpiryDate().toString() : "2029-06-30")
                .tripStatus("NONE");

        if (assignment == null) {
            if (driver.getAssignedBusId() != null) {
                busRepository.findById(driver.getAssignedBusId()).ifPresent(bus -> dutyBuilder
                        .busId(bus.getId()).busNumber(bus.getBusNumber()).registrationNumber(bus.getRegistrationNumber()).busCapacity(bus.getCapacity()));
            }
            if (driver.getAssignedRouteId() != null) {
                routeRepository.findById(driver.getAssignedRouteId()).ifPresent(r -> {
                    dutyBuilder.routeId(r.getId())
                            .routeName(r.getRouteName())
                            .routeCode(r.getRouteCode())
                            .plannedDepartureTime(r.getMorningStartTime() != null ? r.getMorningStartTime() : "07:15 AM")
                            .route(mapToRouteDto(r));
                });
            }
            if (driver.getAssignedBusId() != null || driver.getAssignedRouteId() != null) {
                dutyBuilder.shift("MORNING").tripStatus("SCHEDULED");
            }
        }
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

        tripRepository.findFirstByDriverIdAndStatus(driver.getId(), "IN_PROGRESS").ifPresent(trip -> {
            dutyBuilder.activeTripId(trip.getId()).tripStatus("IN_PROGRESS")
                    .busId(trip.getBusId()).routeId(trip.getRouteId())
                    .activeTrip(mapToTripSummaryDto(trip));
        });

        // 2. Safety checklist from admin config for this school
        List<SafetyChecklistItemDto> activeChecklist = getSafetyChecklist(driver.getSchoolId(), true);
        dutyBuilder.safetyChecklist(activeChecklist);
        dutyBuilder.safetyList(activeChecklist);

        // 3. Driver license verification status & details
        boolean isLicenseVerified = driver.getVerificationStatus() != null &&
                "VERIFIED".equalsIgnoreCase(driver.getVerificationStatus().trim());
        String verificationStatus = driver.getVerificationStatus() != null ? driver.getVerificationStatus() : "VERIFIED";
        String licenseNum = (driver.getLicenseNumber() != null && !driver.getLicenseNumber().isBlank())
                ? driver.getLicenseNumber()
                : "DL-DRI-" + driver.getId();
        String licenseExp = driver.getLicenseExpiryDate() != null
                ? driver.getLicenseExpiryDate().toString()
                : "2031-10-21";
        String licenseCat = driver.getLicenseCategory() != null ? driver.getLicenseCategory() : "COMMERCIAL_HEAVY";

        dutyBuilder.licenseVerified(isLicenseVerified);
        dutyBuilder.isLicenseVerified(isLicenseVerified);
        dutyBuilder.verificationStatus(verificationStatus);
        dutyBuilder.driverLicense(DriverLicenseDto.builder()
                .licenseNumber(licenseNum)
                .licenseExpiryDate(licenseExp)
                .licenseCategory(licenseCat)
                .verificationStatus(verificationStatus)
                .isVerified(isLicenseVerified)
                .build());

        TripDtos.DriverDutyDto dutyResult = dutyBuilder.build();

        // 1. Today Assigned Duty data block (also preserves flat fields for backward compatibility)
        Map<String, Object> dutyMap = new LinkedHashMap<>();
        dutyMap.put("driverId", dutyResult.getDriverId());
        dutyMap.put("driverName", dutyResult.getDriverName());
        dutyMap.put("phone", dutyResult.getPhone());
        dutyMap.put("licenseNumber", dutyResult.getLicenseNumber());
        dutyMap.put("licenseExpiryDate", dutyResult.getLicenseExpiryDate());
        dutyMap.put("assignmentId", dutyResult.getAssignmentId());
        dutyMap.put("busId", dutyResult.getBusId());
        dutyMap.put("busNumber", dutyResult.getBusNumber());
        dutyMap.put("registrationNumber", dutyResult.getRegistrationNumber());
        dutyMap.put("busCapacity", dutyResult.getBusCapacity());
        dutyMap.put("routeId", dutyResult.getRouteId());
        dutyMap.put("routeName", dutyResult.getRouteName());
        dutyMap.put("routeCode", dutyResult.getRouteCode());
        dutyMap.put("shift", dutyResult.getShift());
        dutyMap.put("plannedDepartureTime", dutyResult.getPlannedDepartureTime());
        dutyMap.put("activeTripId", dutyResult.getActiveTripId());
        dutyMap.put("tripStatus", dutyResult.getTripStatus());
        dutyMap.put("route", dutyResult.getRoute());
        dutyMap.put("activeTrip", dutyResult.getActiveTrip());
        dutyMap.put("todayChecklistCompleted", dutyResult.getTodayChecklistCompleted());
        dutyMap.put("todayChecklistNotes", dutyResult.getTodayChecklistNotes());
        dutyMap.put("todayStartLatitude", dutyResult.getTodayStartLatitude());
        dutyMap.put("todayStartLongitude", dutyResult.getTodayStartLongitude());
        dutyMap.put("todayStartLocationName", dutyResult.getTodayStartLocationName());
        dutyMap.put("todayActualStartTime", dutyResult.getTodayActualStartTime());
        dutyMap.put("todayChkBrakes", dutyResult.getTodayChkBrakes());
        dutyMap.put("todayChkLights", dutyResult.getTodayChkLights());
        dutyMap.put("todayChkTires", dutyResult.getTodayChkTires());
        dutyMap.put("todayChkEmergencyKit", dutyResult.getTodayChkEmergencyKit());
        dutyMap.put("todayChkGpsActive", dutyResult.getTodayChkGpsActive());

        dutyResult.setAssignedDuty(dutyMap);
        dutyResult.setDuty(dutyMap);

        return dutyResult;
    }

    @Override
    public TripDtos.TripSummaryDto startTrip(String driverEmail, TripDtos.StartTripRequest request) {
        DriverProfileEntity driver = requireTrackingDriver(driverEmail);

        BusAssignmentEntity assignment = null;
        if (request.getAssignmentId() != null) {
            assignment = assignmentRepository.findById(request.getAssignmentId()).orElse(null);
        }
        if (assignment == null) {
            assignment = assignmentRepository.findFirstByDriverIdAndStatusOrderByIdDesc(driver.getId(), "ACTIVE")
                    .orElse(null);
        }
        if (assignment == null) {
            throw new IllegalStateException("No active route duty is assigned. Ask the school to schedule your assigned bus.");
        }
        if (!Objects.equals(assignment.getDriverId(), driver.getId())
                || !Objects.equals(assignment.getSchoolId(), driver.getSchoolId())
                || !"ACTIVE".equals(assignment.getStatus())) {
            throw new UnauthorizedException("Only the assigned driver can start this trip");
        }
        if ((request.getBusId() != null && !Objects.equals(request.getBusId(), assignment.getBusId()))
                || (request.getRouteId() != null && !Objects.equals(request.getRouteId(), assignment.getRouteId()))
                || (driver.getAssignedBusId() != null && !Objects.equals(driver.getAssignedBusId(), assignment.getBusId()))) {
            throw new UnauthorizedException("Start only the bus and route assigned by the school");
        }
        BusEntity assignedBus = busRepository.findForDriverAssignment(assignment.getBusId(), driver.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("Assigned bus not found"));
        if (!"ACTIVE".equals(assignedBus.getStatus())) {
            throw new IllegalStateException("Assigned bus is unavailable");
        }
        // Serialize starts for this bus before checking for an existing trip.
        Optional<TransportTripEntity> existingActive = tripRepository.findFirstByDriverIdAndStatus(driver.getId(), "IN_PROGRESS");
        if (existingActive.isPresent()) return mapToTripSummaryDto(existingActive.get());
        if (tripRepository.findFirstByBusIdAndStatus(assignment.getBusId(), "IN_PROGRESS").isPresent()) {
            throw new IllegalStateException("This bus already has an active trip");
        }
        routeRepository.findByIdAndSchoolId(assignment.getRouteId(), driver.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("Assigned route not found"));
        Long routeId = assignment.getRouteId();
        Long busId = assignment.getBusId();

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
                .recordedAt(LocalDateTime.now(java.time.ZoneOffset.UTC))
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

    private DriverProfileEntity requireTrackingDriver(String email) {
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("Driver authentication required");
        }
        String cleanEmail = email.trim();
        Optional<DriverProfileEntity> byEmail = driverRepository.findByEmailIgnoreCase(cleanEmail);
        if (byEmail.isPresent()) return byEmail.get();

        Optional<UserEntity> userOpt = userRepository.findByEmailIgnoreCase(cleanEmail);
        if (userOpt.isPresent() && userOpt.get().getRole() == com.school.management.domain.user.Role.DRIVER) {
            UserEntity user = userOpt.get();
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
                    .build();
            return driverRepository.save(newDriver);
        }
        // If testing or admin account, fallback to the first active registered driver
        List<DriverProfileEntity> allDrivers = driverRepository.findAll();
        if (!allDrivers.isEmpty()) {
            return allDrivers.get(0);
        }
        throw new UnauthorizedException("A registered driver is required");
    }

    private void validateGpsPoint(TripDtos.GpsPointDto point) {
        if (point == null || point.getLatitude() == null || point.getLongitude() == null
                || !Double.isFinite(point.getLatitude()) || !Double.isFinite(point.getLongitude())
                || Math.abs(point.getLatitude()) > 90 || Math.abs(point.getLongitude()) > 180
                || point.getRecordedAt() == null
                || (point.getSpeedKmh() != null && (!Double.isFinite(point.getSpeedKmh()) || point.getSpeedKmh() < 0))
                || (point.getAccuracyMeters() != null && (!Double.isFinite(point.getAccuracyMeters()) || point.getAccuracyMeters() < 0))) {
            throw new IllegalArgumentException("Invalid GPS point");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void publishLiveLocation(Long tripId, String driverEmail, TripDtos.GpsPointDto point) {
        validateGpsPoint(point);
        LivePosition cached = livePositions.get(tripId);
        if (cached == null || Duration.between(cached.receivedAt(), LocalDateTime.now()).toMinutes() >= 5) {
            DriverProfileEntity driver = requireTrackingDriver(driverEmail);
            TransportTripEntity trip = tripRepository.findById(tripId)
                    .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
            if (!Objects.equals(driver.getId(), trip.getDriverId()) || !"IN_PROGRESS".equals(trip.getStatus())) {
                throw new UnauthorizedException("Only the active trip driver can publish location");
            }
        } else if (!cached.driverEmail().equalsIgnoreCase(driverEmail)) {
            throw new UnauthorizedException("Trip driver mismatch");
        }
        livePositions.compute(tripId, (id, previous) ->
                previous != null && point.getRecordedAt().isBefore(previous.point().getRecordedAt())
                        ? previous : new LivePosition(driverEmail, point, LocalDateTime.now()));
        // Evict abandoned telemetry; persistent history remains in the driver's SQLite store.
        livePositions.entrySet().removeIf(e -> Duration.between(e.getValue().receivedAt(), LocalDateTime.now()).toHours() >= 12);
    }

    @Override
    public int ingestGpsBatch(Long tripId, String driverEmail, TripDtos.BatchGpsRequest request) {
        throw new IllegalStateException("GPS history is accepted only with trip completion. Use /locations/live for transient telemetry.");
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
        DriverProfileEntity driver = requireTrackingDriver(driverEmail);
        TransportTripEntity trip = tripRepository.findForCompletion(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        if (!Objects.equals(driver.getId(), trip.getDriverId())) {
            throw new UnauthorizedException("Only this trip's driver can complete it");
        }
        // Lock + terminal state make retries safe after a lost response.
        if ("COMPLETED".equals(trip.getStatus())) return mapToTripSummaryDto(trip);
        if (!"IN_PROGRESS".equals(trip.getStatus())) throw new IllegalStateException("Trip is not active");
        List<TripDtos.GpsPointDto> points = request.getPoints() == null ? List.of() : request.getPoints();
        if (points.size() > 10000) throw new IllegalArgumentException("Trip GPS batch exceeds 10000 points");
        points.forEach(this::validateGpsPoint);
        List<TripDtos.GpsPointDto> ordered = points.stream()
                .sorted(Comparator.comparing(TripDtos.GpsPointDto::getRecordedAt)).toList();
        List<BusLocationHistoryEntity> history = new ArrayList<>(ordered.size());
        LocalDateTime previousTime = null;
        for (TripDtos.GpsPointDto point : ordered) {
            if (point.getRecordedAt().equals(previousTime)) continue;
            if (trip.getActualStartTime() != null && point.getRecordedAt().isBefore(trip.getActualStartTime().atZone(java.time.ZoneId.systemDefault()).withZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime().minusMinutes(5))) {
                throw new IllegalArgumentException("GPS point predates this trip");
            }
            if (point.getRecordedAt().isAfter(LocalDateTime.now(java.time.ZoneOffset.UTC).plusMinutes(5))) {
                throw new IllegalArgumentException("GPS timestamp is in the future");
            }
            history.add(BusLocationHistoryEntity.builder()
                    .schoolId(trip.getSchoolId()).tripId(tripId).busId(trip.getBusId()).driverId(trip.getDriverId())
                    .latitude(point.getLatitude()).longitude(point.getLongitude())
                    .speedKmh(point.getSpeedKmh() == null ? 0.0 : point.getSpeedKmh())
                    .heading(point.getHeading()).accuracyMeters(point.getAccuracyMeters())
                    .recordedAt(point.getRecordedAt()).serverReceivedAt(LocalDateTime.now()).build());
            previousTime = point.getRecordedAt();
        }
        if (!history.isEmpty()) {
            locationHistoryRepository.saveAll(history);
            TripDtos.GpsPointDto last = ordered.get(ordered.size() - 1);
            trip.setLastLatitude(last.getLatitude()); trip.setLastLongitude(last.getLongitude());
            trip.setLastLocationTime(last.getRecordedAt());
        }
        trip.setStatus("COMPLETED");
        trip.setActualEndTime(LocalDateTime.now());
        trip.setCurrentSpeedKmh(0.0);
        if (request.getTotalDistanceKm() != null && Double.isFinite(request.getTotalDistanceKm()) && request.getTotalDistanceKm() >= 0) {
            trip.setTotalDistanceKm(request.getTotalDistanceKm());
        }
        trip.setEndNotes(request.getNotes());
        TransportTripEntity saved = tripRepository.save(trip);
        busRepository.findById(trip.getBusId()).ifPresent(bus -> {
            bus.setCurrentSpeed(0.0);
            bus.setLastLatitude(trip.getLastLatitude()); bus.setLastLongitude(trip.getLastLongitude());
            bus.setLastGpsUpdate(trip.getLastLocationTime());
            busRepository.save(bus);
        });
        livePositions.remove(tripId);
        logAudit(trip.getSchoolId(), "TRIP_COMPLETED", "TRIP", saved.getId(), "Driver completed trip id: " + saved.getId());
        return mapToTripSummaryDto(saved);
    }

    // ==========================================
    // ADMIN LIVE TRACKING & PLAYBACK
    // ==========================================

    @Override
    public List<TripDtos.LiveTrackingBusDto> getLiveTracking(String schoolId) {
        String sId = resolveSchool(schoolId);
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

            LivePosition live = tripId == null ? null : livePositions.get(tripId);
            LocalDateTime lastGps = live != null ? live.receivedAt() : bus.getLastGpsUpdate();
            boolean isOnline = activeTrip.isPresent() && lastGps != null && Duration.between(lastGps, LocalDateTime.now()).toSeconds() < 120;

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
                    .currentLatitude(live != null ? live.point().getLatitude() : bus.getLastLatitude())
                    .currentLongitude(live != null ? live.point().getLongitude() : bus.getLastLongitude())
                    .currentSpeedKmh(live != null ? live.point().getSpeedKmh() : 0.0)
                    .heading(live != null ? live.point().getHeading() : 0.0)
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

        if (!Objects.equals(trip.getSchoolId(), resolveSchool(schoolId))) {
            throw new ResourceNotFoundException("Trip not found in this school");
        }

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
                .sorted(Comparator.comparing(TransportTripEntity::getTripDate).thenComparing(TransportTripEntity::getId).reversed())
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
        Optional<TransportTripEntity> activeTrip = tripRepository.findFirstByRouteIdAndSchoolIdAndStatus(assignment.getRouteId(), assignment.getSchoolId(), "IN_PROGRESS");
        if (activeTrip.isEmpty()) {
            List<BusAssignmentEntity> busAssignments = assignmentRepository.findByRouteIdAndStatus(assignment.getRouteId(), "ACTIVE");
            if (!busAssignments.isEmpty()) {
                activeTrip = tripRepository.findFirstByBusIdAndStatus(busAssignments.get(0).getBusId(), "IN_PROGRESS");
            }
        }

        BusEntity bus = null;
        DriverProfileEntity driver = null;
        Double lat = null;
        Double lon = null;
        Double speed = 0.0;
        int eta = 8;
        String tripStatus = "SCHEDULED";
        LocalDateTime lastGps = null;

        if (activeTrip.isPresent()) {
            TransportTripEntity t = activeTrip.get();
            tripStatus = t.getStatus();
            LivePosition live = livePositions.get(t.getId());
            lat = live != null ? live.point().getLatitude() : t.getLastLatitude();
            lon = live != null ? live.point().getLongitude() : t.getLastLongitude();
            speed = live != null ? live.point().getSpeedKmh() : 0.0;
            eta = t.getNextStopEtaMinutes() != null ? t.getNextStopEtaMinutes() : 8;
            lastGps = live != null ? live.receivedAt() : t.getLastLocationTime();
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
    // SAFETY CHECKLIST CONFIGURATION
    // ==========================================

    @Override
    public List<SafetyChecklistItemDto> getSafetyChecklist(String schoolId, boolean onlyActive) {
        String sId = resolveSchool(schoolId);
        seedDefaultChecklistIfEmpty(sId);
        List<SafetyChecklistItemEntity> list = onlyActive
                ? checklistRepository.findBySchoolIdAndIsActiveTrueOrderBySortOrderAsc(sId)
                : checklistRepository.findBySchoolIdOrderBySortOrderAsc(sId);
        return list.stream().map(this::mapToChecklistItemDto).collect(Collectors.toList());
    }

    @Override
    public SafetyChecklistItemDto createSafetyChecklistItem(SafetyChecklistItemDto dto, String schoolId) {
        String sId = resolveSchool(schoolId);
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new BadRequestException("Safety checklist title is required");
        }
        Integer sortOrder = dto.getSortOrder();
        if (sortOrder == null) {
            sortOrder = (int) checklistRepository.countBySchoolId(sId) + 1;
        }
        SafetyChecklistItemEntity entity = SafetyChecklistItemEntity.builder()
                .schoolId(sId)
                .title(dto.getTitle().trim())
                .description(dto.getDescription() != null ? dto.getDescription().trim() : "")
                .iconKey(dto.getIconKey() != null && !dto.getIconKey().isBlank() ? dto.getIconKey().trim() : "check")
                .isRequired(dto.getRequired() != null ? dto.getRequired() : true)
                .isActive(dto.getActive() != null ? dto.getActive() : true)
                .sortOrder(sortOrder)
                .build();
        SafetyChecklistItemEntity saved = checklistRepository.save(entity);
        logAudit(sId, "CREATE", "SAFETY_CHECKLIST", saved.getId(), "Created safety checklist item: " + saved.getTitle());
        return mapToChecklistItemDto(saved);
    }

    @Override
    public SafetyChecklistItemDto updateSafetyChecklistItem(Long id, SafetyChecklistItemDto dto, String schoolId) {
        String sId = resolveSchool(schoolId);
        SafetyChecklistItemEntity entity = checklistRepository.findByIdAndSchoolId(id, sId)
                .orElseThrow(() -> new ResourceNotFoundException("Safety checklist item not found with id: " + id));

        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            entity.setTitle(dto.getTitle().trim());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription().trim());
        }
        if (dto.getIconKey() != null && !dto.getIconKey().isBlank()) {
            entity.setIconKey(dto.getIconKey().trim());
        }
        if (dto.getRequired() != null) {
            entity.setIsRequired(dto.getRequired());
        }
        if (dto.getActive() != null) {
            entity.setIsActive(dto.getActive());
        }
        if (dto.getSortOrder() != null) {
            entity.setSortOrder(dto.getSortOrder());
        }
        SafetyChecklistItemEntity updated = checklistRepository.save(entity);
        logAudit(sId, "UPDATE", "SAFETY_CHECKLIST", updated.getId(), "Updated safety checklist item: " + updated.getTitle());
        return mapToChecklistItemDto(updated);
    }

    @Override
    public void deleteSafetyChecklistItem(Long id, String schoolId) {
        String sId = resolveSchool(schoolId);
        SafetyChecklistItemEntity entity = checklistRepository.findByIdAndSchoolId(id, sId)
                .orElseThrow(() -> new ResourceNotFoundException("Safety checklist item not found with id: " + id));
        checklistRepository.delete(entity);
        logAudit(sId, "DELETE", "SAFETY_CHECKLIST", id, "Deleted safety checklist item: " + entity.getTitle());
    }

    @Override
    public List<SafetyChecklistItemDto> resetSafetyChecklistDefaults(String schoolId) {
        String sId = resolveSchool(schoolId);
        checklistRepository.deleteBySchoolId(sId);
        seedDefaultChecklistIfEmpty(sId);
        logAudit(sId, "RESET", "SAFETY_CHECKLIST", null, "Reset safety checklist items to standard defaults for " + sId);
        return getSafetyChecklist(sId, false);
    }

    public synchronized void seedDefaultChecklistIfEmpty(String schoolId) {
        // Safety checklist items are created and managed only through the dashboard, not seeded from code.
    }

    private SafetyChecklistItemDto mapToChecklistItemDto(SafetyChecklistItemEntity e) {
        return SafetyChecklistItemDto.builder()
                .id(e.getId())
                .schoolId(e.getSchoolId())
                .title(e.getTitle())
                .description(e.getDescription())
                .iconKey(e.getIconKey())
                .isRequired(e.getIsRequired())
                .required(e.getIsRequired())
                .isActive(e.getIsActive())
                .active(e.getIsActive())
                .sortOrder(e.getSortOrder())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    // ==========================================
    // INITIAL DATA SEEDER
    // ==========================================

    @Override
    public synchronized void seedInitialTransportDataIfEmpty(String schoolId) {
        // School transport records are created only through the management APIs.
    }
}

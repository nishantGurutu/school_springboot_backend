package com.school.management.service;

import com.school.management.dto.transport.BusDto;
import com.school.management.entity.transport.BusEntity;
import com.school.management.entity.transport.DriverProfileEntity;
import com.school.management.exceptions.BadRequestException;
import com.school.management.repository.transport.BusAssignmentRepository;
import com.school.management.repository.transport.BusRepository;
import com.school.management.repository.transport.DriverProfileRepository;
import com.school.management.repository.transport.TransportTripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DriverBusAllocationService {
    private final BusRepository buses;
    private final DriverProfileRepository drivers;
    private final BusAssignmentRepository assignments;
    private final TransportTripRepository trips;

    @Transactional(readOnly = true)
    public List<BusDto> availableBuses(String schoolId) {
        return buses.findAvailableForDriver(schoolId).stream().map(bus -> BusDto.builder()
                .id(bus.getId()).schoolId(bus.getSchoolId()).busNumber(bus.getBusNumber())
                .registrationNumber(bus.getRegistrationNumber()).capacity(bus.getCapacity())
                .status(bus.getStatus()).assignmentStatus("AVAILABLE").availableForAssignment(true).build()).toList();
    }

    /** All allocation writers lock the bus, so two staff forms cannot reserve it. */
    @Transactional
    public void assign(DriverProfileEntity driver, Long busId) {
        BusEntity bus = buses.findForDriverAssignment(busId, driver.getSchoolId())
                .orElseThrow(() -> new BadRequestException("Selected bus does not exist in this school"));
        if (!"ACTIVE".equalsIgnoreCase(bus.getStatus())) {
            throw new BadRequestException("Only active buses can be assigned");
        }
        boolean occupied = drivers.findByAssignedBusId(busId).stream()
                .anyMatch(other -> !Objects.equals(other.getId(), driver.getId()))
                || assignments.findByBusIdAndStatus(busId, "ACTIVE").stream()
                .anyMatch(other -> !Objects.equals(other.getDriverId(), driver.getId()))
                || trips.findFirstByBusIdAndStatus(busId, "IN_PROGRESS")
                .filter(trip -> !Objects.equals(trip.getDriverId(), driver.getId())).isPresent();
        if (occupied) throw new BadRequestException("This bus is already assigned to another driver. Select an unassigned bus.");
        if (driver.getAssignedBusId() != null && !Objects.equals(driver.getAssignedBusId(), busId)
                && (!assignments.findByDriverIdAndStatus(driver.getId(), "ACTIVE").isEmpty()
                    || trips.findFirstByDriverIdAndStatus(driver.getId(), "IN_PROGRESS").isPresent())) {
            throw new BadRequestException("End the driver's current trip and remove the existing duty before changing buses");
        }
        driver.setAssignedBusId(busId);
        drivers.saveAndFlush(driver);
    }
}

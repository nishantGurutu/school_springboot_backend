package com.school.management.config;

import com.school.management.domain.teacher.TeacherStatus;
import com.school.management.domain.user.Role;
import com.school.management.entity.StaffEntity;
import com.school.management.entity.TeacherEntity;
import com.school.management.entity.UserEntity;
import com.school.management.entity.transport.DriverProfileEntity;
import com.school.management.entity.GalleryAlbumEntity;
import com.school.management.repository.GalleryAlbumRepository;
import com.school.management.repository.StaffRepository;
import com.school.management.repository.TeacherRepository;
import com.school.management.repository.UserRepository;
import com.school.management.repository.transport.DriverProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StaffRepository staffRepository;
    private final DriverProfileRepository driverRepository;
    private final GalleryAlbumRepository galleryAlbumRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            // Update check constraint to support all enum roles including DRIVER
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
            jdbcTemplate.execute("ALTER TABLE users ADD CONSTRAINT users_role_check " +
                    "CHECK (role IN ('MASTER_ADMIN', 'ADMIN', 'SUPER_ADMIN', 'PRINCIPAL', 'TEACHER', 'STUDENT', 'PARENT', 'STAFF', 'ACCOUNTANT', 'LIBRARIAN', 'DRIVER'))");

            jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS department_id BIGINT");
            jdbcTemplate.execute("ALTER TABLE teachers ADD COLUMN IF NOT EXISTS designation_id BIGINT");
            jdbcTemplate.execute("ALTER TABLE school_designations ADD COLUMN IF NOT EXISTS department_id BIGINT");
            jdbcTemplate.execute("ALTER TABLE school_designations DROP CONSTRAINT IF EXISTS uk_desig_name");

            // Transport Schema Migrations
            jdbcTemplate.execute("ALTER TABLE buses ADD COLUMN IF NOT EXISTS permit_expiry_date DATE");
            jdbcTemplate.execute("ALTER TABLE transport_routes ADD COLUMN IF NOT EXISTS area_locality VARCHAR(128)");
            jdbcTemplate.execute("ALTER TABLE transport_routes ADD COLUMN IF NOT EXISTS fare DOUBLE PRECISION");
            jdbcTemplate.execute("ALTER TABLE driver_profiles ADD COLUMN IF NOT EXISTS joining_date DATE");
            jdbcTemplate.execute("ALTER TABLE student_transport_assignments ADD COLUMN IF NOT EXISTS trip_type VARCHAR(32)");
            jdbcTemplate.execute("ALTER TABLE student_transport_assignments ADD COLUMN IF NOT EXISTS start_date DATE");
            jdbcTemplate.execute("ALTER TABLE student_transport_assignments ADD COLUMN IF NOT EXISTS end_date DATE");
            jdbcTemplate.execute("ALTER TABLE student_transport_assignments ADD COLUMN IF NOT EXISTS monthly_fee DOUBLE PRECISION");
            jdbcTemplate.execute("ALTER TABLE student_transport_assignments ADD COLUMN IF NOT EXISTS fee_status VARCHAR(32)");
        } catch (Exception ignored) {
        }

        // Ensure default Super Admin account exists so admin can access the Admin Panel
        Optional<UserEntity> existingAdmin = userRepository.findByEmailIgnoreCase("admin@school.com");
        if (existingAdmin.isEmpty()) {
            userRepository.save(UserEntity.builder()
                    .email("admin@school.com")
                    .password(passwordEncoder.encode("admin123"))
                    .name("Super Admin")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build());
        }

        // Synchronize existing staff, drivers, and teachers across all tables
        try {
            // 1. Sync StaffEntity -> TeacherEntity & DriverProfileEntity
            for (StaffEntity staff : staffRepository.findAll()) {
                if (staff.getEmail() == null || staff.getEmail().isBlank()) continue;
                String email = staff.getEmail().trim().toLowerCase();
                boolean isDriver = (staff.getRole() != null && staff.getRole().toUpperCase().contains("DRIVER"))
                        || (staff.getStaffType() != null && staff.getStaffType().toLowerCase().contains("driver"))
                        || (staff.getDesignation() != null && staff.getDesignation().toLowerCase().contains("driver"));

                // Sync to teachers table if not exists
                if (teacherRepository.findByEmailIgnoreCase(email).isEmpty()) {
                    String fullName = staff.getName() != null ? staff.getName().trim() : "Staff Member";
                    String[] parts = fullName.split("\\s+", 2);
                    String firstName = parts[0];
                    String lastName = parts.length > 1 && !parts[1].isBlank() ? parts[1] : (isDriver ? "(Driver)" : "(Staff)");

                    TeacherEntity t = TeacherEntity.builder()
                            .employeeId("EMP-STF-" + (staff.getId() != null ? staff.getId() : System.currentTimeMillis()))
                            .firstName(firstName)
                            .lastName(lastName)
                            .email(email)
                            .phone(staff.getPhone() != null && !staff.getPhone().isBlank() ? staff.getPhone() : "+91 98000 00000")
                            .department(isDriver ? "Transport" : "Administration")
                            .subject(isDriver ? "Transport" : "General Staff")
                            .designation(staff.getDesignation() != null ? staff.getDesignation() : (isDriver ? "Bus Driver" : "Staff"))
                            .type(isDriver ? "Driver" : (staff.getStaffType() != null ? staff.getStaffType() : "Staff"))
                            .joiningDate(LocalDate.now())
                            .status(TeacherStatus.ACTIVE)
                            .build();
                    teacherRepository.save(t);
                    System.out.println("DataInitializer: Synced staff " + fullName + " into teachers table.");
                }

                // If Driver, ensure driver_profiles has it
                if (isDriver && driverRepository.findByEmailIgnoreCase(email).isEmpty()) {
                    driverRepository.save(DriverProfileEntity.builder()
                            .schoolId("SCH-001")
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
                            .build());
                    System.out.println("DataInitializer: Synced driver " + staff.getName() + " into driver_profiles table.");
                }
            }

            // 2. Sync DriverProfileEntity -> StaffEntity & TeacherEntity
            for (DriverProfileEntity driver : driverRepository.findAll()) {
                if (driver.getEmail() == null || driver.getEmail().isBlank()) continue;
                String email = driver.getEmail().trim().toLowerCase();
                if ("driver@school.com".equalsIgnoreCase(email) || "suresh.driver@school.com".equalsIgnoreCase(email)) {
                    continue;
                }

                if (staffRepository.findByEmailIgnoreCase(email).isEmpty()) {
                    staffRepository.save(StaffEntity.builder()
                            .name(driver.getName())
                            .email(email)
                            .phone(driver.getPhone())
                            .designation("Bus Driver")
                            .staffType("Driver")
                            .role("DRIVER")
                            .status("Active")
                            .joinDate(driver.getJoiningDate() != null ? driver.getJoiningDate().toString() : LocalDate.now().toString())
                            .build());
                    System.out.println("DataInitializer: Synced driver " + driver.getName() + " into staff table.");
                }

                if (teacherRepository.findByEmailIgnoreCase(email).isEmpty()) {
                    String fullName = driver.getName() != null ? driver.getName().trim() : "Driver";
                    String[] parts = fullName.split("\\s+", 2);
                    String firstName = parts[0];
                    String lastName = parts.length > 1 && !parts[1].isBlank() ? parts[1] : "(Driver)";

                    teacherRepository.save(TeacherEntity.builder()
                            .employeeId("EMP-DRV-" + driver.getId())
                            .firstName(firstName)
                            .lastName(lastName)
                            .email(email)
                            .phone(driver.getPhone() != null && !driver.getPhone().isBlank() ? driver.getPhone() : "+91 98000 00000")
                            .department("Transport")
                            .subject("Transport")
                            .designation("Bus Driver")
                            .type("Driver")
                            .joiningDate(driver.getJoiningDate() != null ? driver.getJoiningDate() : LocalDate.now())
                            .status(TeacherStatus.ACTIVE)
                            .avatar(driver.getAvatarUrl())
                            .build());
                    System.out.println("DataInitializer: Synced driver " + driver.getName() + " into teachers table.");
                }

                // 3. Ensure UserEntity exists for Mobile / Web login with Role.DRIVER
                if (userRepository.findByEmailIgnoreCase(email).isEmpty()) {
                    userRepository.save(UserEntity.builder()
                            .email(email)
                            .name(driver.getName() != null ? driver.getName() : "Driver")
                            .password(passwordEncoder.encode("password123"))
                            .role(Role.DRIVER)
                            .enabled(true)
                            .build());
                    System.out.println("DataInitializer: Created driver login for " + email + " (password123).");
                }
            }
        } catch (Exception e) {
            System.err.println("Notice during staff/driver synchronization: " + e.getMessage());
        }

        // Ensure default Gallery Albums exist for web dashboard and mobile app
        try {
            if (galleryAlbumRepository.count() == 0) {
                galleryAlbumRepository.save(GalleryAlbumEntity.builder()
                        .title("Annual Sports Meet 2026")
                        .description("Thrilling track and field competitions, relay races, and award ceremonies celebrating student athletics.")
                        .coverImage("https://images.unsplash.com/photo-1576995853123-5a10305d93c0?w=800&auto=format&fit=crop&q=80")
                        .photoCount(24)
                        .status("Active")
                        .displayOrder(1)
                        .build());

                galleryAlbumRepository.save(GalleryAlbumEntity.builder()
                        .title("Science & Tech Fair")
                        .description("Innovative robotics projects, chemistry experiments, and eco-sustainable models presented by students.")
                        .coverImage("https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=800&auto=format&fit=crop&q=80")
                        .photoCount(16)
                        .status("Active")
                        .displayOrder(2)
                        .build());

                galleryAlbumRepository.save(GalleryAlbumEntity.builder()
                        .title("Cultural Day & Arts Gala")
                        .description("Vibrant folk dances, theatrical performances, musical orchestras, and fine art displays.")
                        .coverImage("https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800&auto=format&fit=crop&q=80")
                        .photoCount(32)
                        .status("Active")
                        .displayOrder(3)
                        .build());

                galleryAlbumRepository.save(GalleryAlbumEntity.builder()
                        .title("Smart Campus & Library")
                        .description("State-of-the-art STEM labs, digital library, sports arena, and green eco-friendly school campus.")
                        .coverImage("https://images.unsplash.com/photo-1580582932707-520aed937b7b?w=800&auto=format&fit=crop&q=80")
                        .photoCount(12)
                        .status("Active")
                        .displayOrder(4)
                        .build());
                System.out.println("DataInitializer: Seeded default active gallery albums.");
            }
        } catch (Exception e) {
            System.err.println("Notice during gallery seeding: " + e.getMessage());
        }

        System.out.println("========================================");
        System.out.println("  SYSTEM INITIALIZED & SYNCHRONIZED:");
        System.out.println("  Super Admin : admin@school.com / admin123 (Role: ADMIN)");
        System.out.println("  Staff, Teachers, and Drivers unified across all modules.");
        System.out.println("========================================");
    }
}

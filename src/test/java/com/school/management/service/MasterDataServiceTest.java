package com.school.management.service;

import com.school.management.domain.teacher.JobType;
import com.school.management.domain.teacher.TeacherStatus;
import com.school.management.domain.user.Role;
import com.school.management.dto.MasterDataResponse;
import com.school.management.entity.StaffEntity;
import com.school.management.entity.TeacherEntity;
import com.school.management.entity.UserEntity;
import com.school.management.repository.*;
import com.school.management.service.impl.MasterDataServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MasterDataServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private GuardianRepository guardianRepository;
    @Mock
    private SchoolClassRepository schoolClassRepository;
    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private MasterDataServiceImpl masterDataService;

    private UserEntity teacherUser;
    private TeacherEntity teacherEntity;

    @BeforeEach
    void setUp() {
        teacherUser = UserEntity.builder()
                .id(10L)
                .email("teacher@school.com")
                .name("John Doe")
                .role(Role.TEACHER)
                .enabled(true)
                .build();

        teacherEntity = TeacherEntity.builder()
                .id(50L)
                .employeeId("EMP-101")
                .firstName("John")
                .lastName("Doe")
                .department("Science")
                .departmentId(2L)
                .subject("Physics")
                .qualification("M.Sc Physics, B.Ed")
                .designation("Senior Teacher")
                .phone("+91 9876543210")
                .email("teacher@school.com")
                .address("123 Street, City")
                .joiningDate(LocalDate.of(2022, 1, 15))
                .experienceYears(6)
                .bloodGroup("O+")
                .jobType(JobType.FULL_TIME)
                .status(TeacherStatus.ACTIVE)
                .gender("Male")
                .dob(LocalDate.of(1990, 5, 20))
                .fatherName("Robert Doe")
                .motherName("Mary Doe")
                .maritalStatus("Married")
                .contractType("Permanent")
                .shift("Morning")
                .workLocation("Main Campus")
                .bankAccountNumber("987654321012")
                .bankName("State Bank of India")
                .ifscCode("SBIN0001234")
                .nationalIdNumber("1234-5678-9012")
                .prevSchoolName("St. Xavier High School")
                .prevSchoolAddress("Mumbai")
                .permanentAddress("456 Avenue, City")
                .teacherBio("Dedicated physics teacher with 6 years experience")
                .build();
    }

    @Test
    void testGetMasterData_ForTeacher_PopulatesAllFields() {
        when(teacherRepository.findByEmailIgnoreCase("teacher@school.com"))
                .thenReturn(Optional.of(teacherEntity));
        when(staffRepository.findByEmailIgnoreCase("teacher@school.com"))
                .thenReturn(Optional.of(StaffEntity.builder().salary(45000.0).build()));

        MasterDataResponse response = masterDataService.getMasterData(teacherUser);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getUserDetails());
        assertNotNull(response.getSettings());
        assertTrue(response.getSettings().isEmpty(), "Settings should currently be empty map ready for future expansion");

        var details = response.getUserDetails();
        assertEquals(10L, details.get("userId"));
        assertEquals("teacher@school.com", details.get("email"));
        assertEquals("John Doe", details.get("name"));
        assertEquals("TEACHER", details.get("role"));
        assertEquals("EMP-101", details.get("employeeId"));
        assertEquals("Science", details.get("department"));
        assertEquals("Physics", details.get("subject"));
        assertEquals("Senior Teacher", details.get("designation"));
        assertEquals("M.Sc Physics, B.Ed", details.get("qualification"));
        assertEquals("+91 9876543210", details.get("phone"));
        assertEquals("Robert Doe", details.get("fatherName"));
        assertEquals("Mary Doe", details.get("motherName"));
        assertEquals("Married", details.get("maritalStatus"));
        assertEquals("Permanent", details.get("contractType"));
        assertEquals("987654321012", details.get("bankAccountNumber"));
        assertEquals("State Bank of India", details.get("bankName"));
        assertEquals("SBIN0001234", details.get("ifscCode"));
        assertEquals("1234-5678-9012", details.get("nationalIdNumber"));
        assertEquals("St. Xavier High School", details.get("prevSchoolName"));
        assertEquals(45000.0, details.get("salary"));
        assertEquals("Dedicated physics teacher with 6 years experience", details.get("teacherBio"));
    }

    @Test
    void testGetMasterData_ForStaff_PopulatesStaffFields() {
        UserEntity staffUser = UserEntity.builder()
                .id(20L)
                .email("accountant@school.com")
                .name("Alice Smith")
                .role(Role.ACCOUNTANT)
                .enabled(true)
                .build();

        StaffEntity staffEntity = StaffEntity.builder()
                .id(60L)
                .name("Alice Smith")
                .email("accountant@school.com")
                .phone("+91 9876543211")
                .staffType("Accounts")
                .designation("Senior Accountant")
                .salary(50000.0)
                .joinDate("2021-08-01")
                .status("Active")
                .role("ACCOUNTANT")
                .build();

        when(staffRepository.findByEmailIgnoreCase("accountant@school.com"))
                .thenReturn(Optional.of(staffEntity));
        when(teacherRepository.findByEmailIgnoreCase("accountant@school.com"))
                .thenReturn(Optional.empty());

        MasterDataResponse response = masterDataService.getMasterData(staffUser);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        var details = response.getUserDetails();
        assertEquals(20L, details.get("userId"));
        assertEquals("accountant@school.com", details.get("email"));
        assertEquals("Alice Smith", details.get("name"));
        assertEquals("ACCOUNTANT", details.get("role"));
        assertEquals("Accounts", details.get("staffType"));
        assertEquals("Senior Accountant", details.get("designation"));
        assertEquals(50000.0, details.get("salary"));
        assertEquals("Active", details.get("status"));
    }
}

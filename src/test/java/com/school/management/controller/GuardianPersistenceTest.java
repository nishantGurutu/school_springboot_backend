package com.school.management.controller;

import com.school.management.domain.guardian.GuardianType;
import com.school.management.domain.student.Gender;
import com.school.management.domain.user.Role;
import com.school.management.dto.GuardianRequest;
import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.util.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class GuardianPersistenceTest {
    @Autowired GuardianController dashboard;
    @Autowired MobileAppController mobile;
    @Autowired StudentRepository students;
    @Autowired GuardianRepository guardians;
    @MockBean SecurityUtil security;

    @Test void dashboardGuardiansAppearForEveryLinkedStudentAndTrackEditsAndDeletes() {
        StudentEntity student = students.save(StudentEntity.builder().admissionNo("GF-1").name("Flow Student")
                .firstName("Flow").lastName("Student").rollNo("1").className("7").section("B")
                .gender(Gender.MALE).dateOfBirth(LocalDate.of(2014, 1, 1)).phone("1234567890")
                .email("guardian.flow.student@test.local").build());
        students.save(StudentEntity.builder().admissionNo("GF-10").name("Other Student")
                .firstName("Other").lastName("Student").rollNo("2").className("7").section("B")
                .gender(Gender.MALE).dateOfBirth(LocalDate.of(2014, 1, 1)).phone("1234567891")
                .email("guardian.flow.other@test.local").build());
        GuardianRequest first = request("Asha", "asha.gf@test.local", "GF-1", GuardianType.MOTHER);
        var mother = dashboard.create(first).getBody();
        var father = dashboard.create(request("Ravi", "ravi.gf@test.local", "GF-10, GF-1", GuardianType.FATHER)).getBody();
        dashboard.create(request("Other", "other.gf@test.local", "GF-10", GuardianType.OTHER));
        when(security.getCurrentUser()).thenReturn(Optional.of(UserEntity.builder().role(Role.STUDENT).email(student.getEmail()).build()));
        var list = mobile.getStudentGuardians(null).getBody();
        assertEquals(List.of(mother.getId(), father.getId()), list.stream().map(g -> g.getId()).toList());
        assertEquals("1234567899", list.get(0).getPhone());
        assertEquals("Family address", list.get(0).getAddress());
        first.setPhone("9999999999");
        dashboard.update(mother.getId(), first);
        assertEquals("9999999999", mobile.getStudentGuardians(null).getBody().get(0).getPhone());
        dashboard.delete(mother.getId());
        assertEquals(List.of(father.getId()), mobile.getStudentGuardians(null).getBody().stream().map(g -> g.getId()).toList());
        assertTrue(guardians.findLinkedToStudent("gf-1").stream().anyMatch(g -> g.getId().equals(father.getId())));
    }

    private GuardianRequest request(String name, String email, String admission, GuardianType type) {
        GuardianRequest request = new GuardianRequest();
        request.setGuardianName(name);
        request.setEmail(email);
        request.setStudentAdmissionNo(admission);
        request.setGuardianType(type);
        request.setPhone("1234567899");
        request.setOccupation("Teacher");
        request.setAddress("Family address");
        return request;
    }
}

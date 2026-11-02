package com.school.management.controller;

import com.school.management.dto.GuardianRequest;
import com.school.management.dto.StudentRequest;
import com.school.management.exceptions.BadRequestException;
import com.school.management.repository.GuardianRepository;
import com.school.management.service.GuardianService;
import com.school.management.service.StudentService;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AdmissionFormRulesTest {
    @Autowired GuardianService guardians;
    @Autowired StudentService students;
    @Autowired GuardianRepository guardianRepository;
    @Autowired Validator validator;

    private GuardianRequest guardian(String admission) {
        GuardianRequest request = new GuardianRequest();
        request.setGuardianName("Required Child Parent");
        request.setEmail("required.child@test.local");
        request.setStudentAdmissionNo(admission);
        return request;
    }

    private StudentRequest student(String roll) {
        StudentRequest request = new StudentRequest();
        request.setFullName("Optional Fields Student");
        request.setStudentClass("7");
        request.setRollNumber(roll);
        request.setPhone("9999999999");
        request.setGender(com.school.management.domain.student.Gender.MALE);
        request.setDob("2014-01-01");
        return request;
    }

    @Test void missingChildIsRejectedByBothDtoAndServiceWithoutSaving() {
        long count = guardianRepository.count();
        for (String admission : new String[] {null, "", "   "}) {
            assertFalse(validator.validate(guardian(admission)).isEmpty());
            assertThrows(BadRequestException.class, () -> guardians.create(guardian(admission)));
        }
        assertEquals(count, guardianRepository.count());
    }

    @Test void malformedAndUnknownChildReferencesCannotCreateGuardian() {
        long count = guardianRepository.count();
        for (String admission : new String[] {",", ", ,", "MISSING-CHILD"}) {
            assertThrows(BadRequestException.class, () -> guardians.create(guardian(admission)));
        }
        assertEquals(count, guardianRepository.count());
    }

    @Test void studentCanBeCreatedWithoutOptionalFieldsAndGetsUniqueAdmissionNumber() {
        StudentRequest first = student("1");
        assertTrue(validator.validate(first).isEmpty());
        var created = students.create(first);
        StudentRequest second = student("2");
        second.setAdmissionNo("   ");
        var other = students.create(second);
        assertNotNull(created.getAdmissionNo());
        assertTrue(created.getAdmissionNo().startsWith("ADM-"));
        assertTrue(created.getAdmissionNo().length() <= 32);
        assertNotEquals(created.getAdmissionNo(), other.getAdmissionNo());
        assertNull(created.getCategory());
        assertNull(created.getBankName());
        assertNull(created.getPrevSchoolName());
        assertNull(created.getHostelName());
        assertNull(created.getDocFile());
        var updated = students.update(created.getId(), student("1"));
        assertEquals(created.getAdmissionNo(), updated.getAdmissionNo());
    }

    @Test void guardianCannotBeUnlinkedByUpdatingWithBlankChild() {
        var child = students.create(student("3"));
        var parent = guardians.create(guardian(child.getAdmissionNo()));
        assertThrows(BadRequestException.class, () -> guardians.update(parent.getId(), guardian("")));
        assertEquals(child.getAdmissionNo(), guardians.getById(parent.getId()).getStudentAdmissionNo());
    }
}

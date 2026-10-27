package com.school.management.controller;

import com.school.management.domain.user.Role;
import com.school.management.domain.student.Gender;
import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.service.FileStorageService;
import com.school.management.util.SecurityUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc(addFilters=false) @Transactional
class StudentReportFlowTest {
    @Autowired MockMvc mvc;
    @Autowired StudentRepository students;
    @Autowired ObjectMapper json;
    @MockBean SecurityUtil security;
    @MockBean FileStorageService files;
    StudentEntity own, other;
    final byte[] pdf="%PDF-1.4 report".getBytes();

    @BeforeEach void setup() {
        own=student("RPT-1","reports.own@test.local","1");
        other=student("RPT-2","reports.other@test.local","2");
        login(Role.ADMIN,"admin@test.local");
    }
    StudentEntity student(String admission,String email,String roll) {
        return students.save(StudentEntity.builder().admissionNo(admission).email(email).name("Report Student "+roll)
                .firstName("Report").lastName("Student").className("Report Class").section("A").rollNo(roll)
                .gender(Gender.MALE).dateOfBirth(LocalDate.of(2014,1,1)).phone("1234567890").build());
    }
    void login(Role role,String email) {
        when(security.getCurrentUser()).thenReturn(Optional.of(UserEntity.builder().id(100L).role(role).email(email).build()));
    }
    long publish() throws Exception {
        when(files.storeFile(any(),eq("reports"))).thenReturn(new FileStorageService.UploadResult("term.pdf","private.pdf","unused","PDF",14L,"14 B"));
        String body=mvc.perform(multipart("/api/student-reports")
                .file(new MockMultipartFile("file","term.pdf","application/pdf",pdf))
                .param("admissionNo",own.getAdmissionNo()).param("title","Term 1 report").param("term","2026 Term 1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.studentName").value(own.getName()))
                .andExpect(jsonPath("$.storedFileName").doesNotExist()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }
    @Test void dashboardUploadAppearsOnlyForSelectedStudentAndPdfIsProtected() throws Exception {
        long id=publish();
        when(files.loadFileAsResource("reports/private.pdf")).thenReturn(new ByteArrayResource(pdf));
        login(Role.STUDENT,own.getEmail());
        mvc.perform(get("/api/student-reports")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fileUrl").value("/api/student-reports/"+id+"/pdf"));
        mvc.perform(get("/api/student-reports/"+id+"/pdf")).andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf")).andExpect(content().bytes(pdf));
        login(Role.STUDENT,other.getEmail());
        mvc.perform(get("/api/student-reports")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/student-reports").param("admissionNo",own.getAdmissionNo())).andExpect(status().isForbidden());
        mvc.perform(get("/api/student-reports/"+id+"/pdf")).andExpect(status().isForbidden());
    }
    @Test void studentCannotPublishOrDeleteAndDashboardDeleteRevokesPdf() throws Exception {
        long id=publish();
        login(Role.STUDENT,own.getEmail());
        mvc.perform(delete("/api/student-reports/"+id)).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/student-reports").file(new MockMultipartFile("file","x.pdf","application/pdf",pdf))
                .param("admissionNo",own.getAdmissionNo()).param("title","Forged")).andExpect(status().isForbidden());
        login(Role.ADMIN,"admin@test.local");
        mvc.perform(delete("/api/student-reports/"+id)).andExpect(status().isOk());
        login(Role.STUDENT,own.getEmail());
        mvc.perform(get("/api/student-reports/"+id+"/pdf")).andExpect(status().isNotFound());
        mvc.perform(get("/api/student-reports")).andExpect(jsonPath("$.length()").value(0));
    }
    @Test void invalidPdfAndUnknownStudentAreRejectedBeforeFileStorage() throws Exception {
        mvc.perform(multipart("/api/student-reports").file(new MockMultipartFile("file","fake.pdf","application/pdf","not a pdf".getBytes()))
                .param("admissionNo",own.getAdmissionNo()).param("title","Fake")).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/student-reports").file(new MockMultipartFile("file","x.pdf","application/pdf",pdf))
                .param("admissionNo","UNKNOWN").param("title","Unknown")).andExpect(status().isNotFound());
        verify(files,never()).storeFile(any(),anyString());
    }
}

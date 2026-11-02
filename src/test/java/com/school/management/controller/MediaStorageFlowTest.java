package com.school.management.controller;

import com.school.management.dto.*;
import com.school.management.domain.student.Gender;
import com.school.management.service.*;
import com.school.management.repository.StoredMediaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDate;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class MediaStorageFlowTest {
    @Autowired MediaStorageService media;
    @Autowired MediaController uploads;
    @Autowired StoredMediaRepository repository;
    @Autowired StudentService students;
    @Autowired GuardianService guardians;
    @Autowired TeacherService teachers;
    @Autowired MockMvc mvc;

    private MockMultipartFile photo(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Random random = new Random(1);
        for (int y=0;y<height;y++) for(int x=0;x<width;x++) image.setRGB(x,y,random.nextInt(0xffffff));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image,"png",bytes); image.flush();
        return new MockMultipartFile("file","photo.png","image/png",bytes.toByteArray());
    }
    private StudentRequest student() {
        StudentRequest r=new StudentRequest();r.setFullName("Media Student");r.setStudentClass("7");r.setRollNumber("10");
        r.setPhone("9999999999");r.setGender(Gender.MALE);r.setDob("2014-01-01");return r;
    }

    @Test void compressesImagePersistsBytesAndServesExactSavedContent() throws Exception {
        var input=photo(2000,1200);
        var result=uploads.upload(input,"photo");
        assertEquals(201,result.getStatusCode().value());
        String id=result.getBody().get("id").toString();
        var saved=repository.findById(id).orElseThrow();
        assertTrue(saved.getContent().length < input.getSize());
        var decoded=ImageIO.read(new ByteArrayInputStream(saved.getContent()));
        assertEquals(1600,decoded.getWidth()); assertEquals(960,decoded.getHeight());
        assertEquals("image/jpeg",saved.getContentType());
        mvc.perform(get("/api/files/media/"+id)).andExpect(status().isOk())
            .andExpect(content().contentType("image/jpeg")).andExpect(content().bytes(saved.getContent()))
            .andExpect(header().string("X-Content-Type-Options","nosniff"));
    }

    @Test void studentParentAndStaffGetApisReturnSavedReferences() throws Exception {
        var asset=media.save(photo(80,60),"photo");
        String url="/api/files/media/"+asset.getId();
        var document=media.save(new MockMultipartFile("file","proof.pdf","application/pdf","%PDF-1.4\nexample".getBytes()),"document");
        String doc="/api/files/media/"+document.getId();
        StudentRequest s=student();s.setStudentPhoto(url);s.setFatherPhoto(url);s.setMotherPhoto(url);s.setGuardianPhoto(url);s.setDocFile(doc);
        var child=students.create(s);
        var got=students.getById(child.getId());
        assertEquals(url,got.getStudentPhoto());assertEquals(url,got.getFatherPhoto());assertEquals(url,got.getMotherPhoto());assertEquals(url,got.getGuardianPhoto());assertEquals(doc,got.getDocFile());
        GuardianRequest g=new GuardianRequest();g.setGuardianName("Media Parent");g.setEmail("media.parent@test.local");g.setStudentAdmissionNo(child.getAdmissionNo());g.setPhoto(url);g.setDocFile(doc);
        var parent=guardians.create(g);assertEquals(url,guardians.getById(parent.getId()).getPhoto());assertEquals(doc,guardians.getById(parent.getId()).getDocFile());
        TeacherRequest t=new TeacherRequest();t.setEmployeeId("MEDIA-STF-1");t.setFirstName("Media");t.setLastName("Staff");t.setDepartment("Administration");t.setEmail("media.staff@test.local");t.setPhone("9999999998");t.setJoiningDate(LocalDate.now());t.setRole("STAFF");t.setAvatar(url);t.setDocFile(doc);
        var staff=teachers.create(t);assertEquals(url,teachers.getById(staff.getId()).getAvatar());assertEquals(doc,teachers.getById(staff.getId()).getDocFile());
        assertTrue(students.getAll(0,20,"id","asc").getContent().stream().anyMatch(row->url.equals(row.getStudentPhoto())));
        assertTrue(guardians.getAll(0,20,"id","asc").getContent().stream().anyMatch(row->doc.equals(row.getDocFile())));
        assertTrue(teachers.getAll(0,20,"id","asc").getContent().stream().anyMatch(row->doc.equals(row.getDocFile())));
    }

    @Test void studentParentPhotoAlsoAppearsInGuardianDirectory() throws Exception {
        String url = "/api/files/media/" + media.save(photo(80,60),"photo").getId();
        StudentRequest request = student();request.setGuardianName("Auto Parent");request.setGuardianEmail("auto.media.parent@test.local");
        request.setGuardianRelation("Father");request.setFatherPhoto(url);
        students.create(request);
        assertEquals(url, guardians.getByEmail("auto.media.parent@test.local").getPhoto());
    }

    @Test void wordDocumentIsStoredWithoutLossAndServedAsDownload() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (var zip = new java.util.zip.ZipOutputStream(bytes)) {
            for (String name : new String[]{"[Content_Types].xml","word/document.xml"}) {
                zip.putNextEntry(new java.util.zip.ZipEntry(name));zip.write("<document>Readable text</document>".getBytes());zip.closeEntry();
            }
        }
        var saved = media.save(new MockMultipartFile("file","proof.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",bytes.toByteArray()),"document");
        assertArrayEquals(bytes.toByteArray(),saved.getContent());
        mvc.perform(get("/api/files/media/"+saved.getId())).andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.startsWith("attachment;")));
    }

    @Test void pdfDocumentIsStoredLosslesslyAndOptionalUploadsCreateNoRows() {
        byte[] pdf="%PDF-1.4\nreadable-document-content".getBytes();
        var saved=media.save(new MockMultipartFile("file","document.pdf","application/pdf",pdf),"document");
        assertArrayEquals(pdf,media.get(saved.getId()).getContent());assertEquals("application/pdf",saved.getContentType());
        long count=repository.count();
        students.create(student());assertEquals(count,repository.count());
    }

    @Test void rejectsInvalidOrOversizeUploadsAndMissingAssets() throws Exception {
        long before=repository.count();
        assertThrows(ResponseStatusException.class,()->media.save(new MockMultipartFile("file","x.png","image/png",new byte[0]),"photo"));
        assertThrows(ResponseStatusException.class,()->media.save(new MockMultipartFile("file","x.png","image/png","<svg><script/></svg>".getBytes()),"photo"));
        assertThrows(ResponseStatusException.class,()->media.save(new MockMultipartFile("file","x.pdf","application/pdf",new byte[8*1024*1024+1]),"document"));
        assertThrows(ResponseStatusException.class,()->media.get("missing"));
        assertEquals(before,repository.count());
        mvc.perform(multipart("/api/files/media").file(photo(20,20))).andExpect(status().isUnauthorized());
    }
}

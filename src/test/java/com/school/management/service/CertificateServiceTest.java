package com.school.management.service;
import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CertificateServiceTest {
 @Test void admissionControlsListAndPdfAccess() {
  var repo=mock(CertificateRepository.class);
  var access=mock(PortalAccessService.class);
  var service=new CertificateService(repo,access,mock(StudentRepository.class));
  when(access.children()).thenReturn(List.of(StudentEntity.builder().admissionNo("OWN").name("Rudra").build()));
  var own=CertificateEntity.builder().id(1L).admissionNo("OWN").name("Rudra").build();
  var other=CertificateEntity.builder().id(2L).admissionNo("OTHER").name("Rudra").build();
  when(repo.findAll()).thenReturn(List.of(own,other,CertificateEntity.builder().name("Rudra").build()));
  assertEquals(1,service.getAll(null).size());
  assertTrue(service.getAll("OTHER").isEmpty());
  when(repo.findById(2L)).thenReturn(Optional.of(other));
  assertThrows(ResourceNotFoundException.class,()->service.getEntityById(2L));
  when(access.children()).thenReturn(List.of());
  assertTrue(service.getAll(null).isEmpty());
 }
 @Test void pdfUsesEnteredNamesOnOnePage() throws Exception {
  var cert=CertificateEntity.builder().name("Rudra").certificateName("Appreciation").className("Play (A)").date("23/10/2026").footerLeft("Anil (Principal)").footerRight("Meera (Class Teacher)").build();
  byte[] bytes = new CertificatePdfService().generateCertificatePdf(cert);
  java.nio.file.Files.write(java.nio.file.Path.of("target/certificate-preview.pdf"), bytes);
  var reader=new com.lowagie.text.pdf.PdfReader(bytes);
  assertEquals(1,reader.getNumberOfPages());
  var text=new com.lowagie.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(1);
  assertTrue(text.contains("Rudra")); assertTrue(text.contains("Anil")); assertTrue(text.contains("Meera")); assertFalse(text.contains("Sarah Jenkins"));
  reader.close();
 }
}

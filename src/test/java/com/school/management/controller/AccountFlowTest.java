package com.school.management.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.user.Role;
import com.school.management.entity.UserEntity;
import com.school.management.repository.UserRepository;
import com.school.management.security.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @Transactional
class AccountFlowTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UserRepository users; @Autowired JwtService jwt;
 String auth;
 @BeforeEach void setup(){var user=users.save(UserEntity.builder().email("account.flow@test.local").name("Account Flow").password("unused").role(Role.ACCOUNTANT).build());auth="Bearer "+jwt.generateAccessToken(user.getEmail());}
 Map<String,Object> transaction(double amount){return new HashMap<>(Map.of("name","Actual income","transactionType","INCOME","category","Flow Head","paymentType","UPI","amount",amount,"description","School income","transactionDate","2026-10-28"));}
 @Test void transactionCreateEditListSummaryDeletePreserveAllFields()throws Exception{
  String body=mvc.perform(post("/api/accounts/transactions").header("Authorization",auth).contentType("application/json").content(json.writeValueAsString(transaction(250))))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Actual income")).andExpect(jsonPath("$.paymentType").value("UPI")).andReturn().getResponse().getContentAsString();
  long id=json.readTree(body).get("id").asLong();
  mvc.perform(put("/api/accounts/transactions/"+id).header("Authorization",auth).contentType("application/json").content(json.writeValueAsString(transaction(300))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(300));
  mvc.perform(get("/api/accounts/transactions").header("Authorization",auth)).andExpect(status().isOk());
  mvc.perform(get("/api/accounts/summary").header("Authorization",auth)).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalIncome").isNumber());
  mvc.perform(delete("/api/accounts/transactions/"+id).header("Authorization",auth)).andExpect(status().isNoContent());
  mvc.perform(put("/api/accounts/transactions/"+id).header("Authorization",auth).contentType("application/json").content(json.writeValueAsString(transaction(300)))).andExpect(status().isNotFound());
 }
 @Test void accountHeadsArePersistedEditableAndDuplicateNamesAreRejected()throws Exception{
  var data=new HashMap<String,Object>(Map.of("name","Flow Head","transactionType","INCOME","description","Fee income","section","General","status","Active"));
  String body=mvc.perform(post("/api/accounts/heads").header("Authorization",auth).contentType("application/json").content(json.writeValueAsString(data)))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.section").value("General")).andExpect(jsonPath("$.description").value("Fee income")).andReturn().getResponse().getContentAsString();
  long id=json.readTree(body).get("id").asLong();
  mvc.perform(post("/api/accounts/heads").header("Authorization",auth).contentType("application/json").content(json.writeValueAsString(data))).andExpect(status().isConflict());
  data.put("description","Updated description");data.put("status","Inactive");
  mvc.perform(put("/api/accounts/heads/"+id).header("Authorization",auth).contentType("application/json").content(json.writeValueAsString(data)))
   .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("Inactive"));
  mvc.perform(get("/api/accounts/heads").header("Authorization",auth)).andExpect(status().isOk());
  mvc.perform(delete("/api/accounts/heads/"+id).header("Authorization",auth)).andExpect(status().isNoContent());
 }
 @Test void invalidAmountsAreRejectedAndTeacherCannotWriteAccounts()throws Exception{
  mvc.perform(post("/api/accounts/transactions").header("Authorization",auth).contentType("application/json").content(json.writeValueAsString(transaction(-1)))).andExpect(status().isBadRequest());
  var teacher=users.save(UserEntity.builder().email("account.teacher@test.local").name("Teacher").password("unused").role(Role.TEACHER).build());
  mvc.perform(post("/api/accounts/heads").header("Authorization","Bearer "+jwt.generateAccessToken(teacher.getEmail())).contentType("application/json").content("{}"))
   .andExpect(status().isForbidden());
 }
}

package com.school.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.user.Role;
import com.school.management.entity.UserEntity;
import com.school.management.repository.*;
import com.school.management.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountantLoginFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired StaffRepository staff;
    @Autowired JwtService jwt;

    Map<String,Object> accountant() {
        var data = new java.util.HashMap<String,Object>(Map.of(
            "employeeId", "ACC-FLOW-42", "firstName", "Ritik", "lastName", "dev",
            "email", "accountant.flow@test.local", "phone", "7238432423",
            "department", "Accounts & Finance (FIN)", "designation", "Accountant (ACCT)",
            "role", "ACCOUNTANT", "type", "Accountant", "password", "FlowSecret42!"));
        data.put("joiningDate", "2026-10-28");
        return data;
    }
    String adminToken() {
        var admin = users.save(UserEntity.builder().email("accountant.flow.admin@test.local")
            .password("unused").name("Flow Admin").role(Role.MASTER_ADMIN).build());
        return jwt.generateAccessToken(admin.getEmail());
    }
    String login(String id, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType("application/json")
            .content(json.writeValueAsString(Map.of("email",id,"password",password))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ACCOUNTANT"))
            .andExpect(jsonPath("$.accessToken").isNotEmpty()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("accessToken").asText();
    }
    @Test void createLoginByEmailEmployeeIdAndPhoneThenCheckPermissions() throws Exception {
        var request = accountant();
        mvc.perform(post("/api/teachers").header("Authorization","Bearer " + adminToken())
            .contentType("application/json").content(json.writeValueAsString(request)))
            .andExpect(status().isCreated());
        assertEquals(1, staff.findAll().stream().filter(s -> request.get("email").equals(s.getEmail())).count());
        String token = login(" ACCOUNTANT.FLOW@TEST.LOCAL ", "FlowSecret42!");
        login("acc-flow-42", "FlowSecret42!");
        login("7238432423", "FlowSecret42!");
        mvc.perform(get("/api/accounts/summary").header("Authorization","Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/fees/collections").header("Authorization","Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/teachers").header("Authorization","Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/hrm/summary").header("Authorization","Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").contentType("application/json")
            .content(json.writeValueAsString(Map.of("email","ACC-FLOW-42","password","wrong"))))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.accessToken").doesNotExist());
    }
    @Test void duplicateStaffRegistrationIsRejectedWithoutBreakingLogin() throws Exception {
        String admin = adminToken();
        mvc.perform(post("/api/teachers").header("Authorization", "Bearer " + admin)
            .contentType("application/json").content(json.writeValueAsString(accountant())))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/staff").header("Authorization", "Bearer " + admin)
            .contentType("application/json").content(json.writeValueAsString(Map.of(
                "name", "Ritik dev", "email", " ACCOUNTANT.FLOW@TEST.LOCAL ",
                "phone", "7238432423", "role", "ACCOUNTANT", "password", "OtherPassword!"))))
            .andExpect(status().isConflict());
        assertEquals(1, staff.findAll().stream().filter(st -> "accountant.flow@test.local".equals(st.getEmail())).count());
        login("accountant.flow@test.local", "FlowSecret42!");
    }
    @Test void editWithoutPasswordKeepsPasswordAndExplicitChangeReplacesIt() throws Exception {
        String admin = adminToken();
        var request = accountant();
        String created = mvc.perform(post("/api/teachers").header("Authorization","Bearer " + admin)
            .contentType("application/json").content(json.writeValueAsString(request)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();
        request.remove("password");
        mvc.perform(put("/api/teachers/" + id).header("Authorization","Bearer " + admin)
            .contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isOk());
        login("ACC-FLOW-42", "FlowSecret42!");
        request.put("password", "ChangedSecret42!");
        mvc.perform(put("/api/teachers/" + id).header("Authorization","Bearer " + admin)
            .contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isOk());
        login("ACC-FLOW-42", "ChangedSecret42!");
        mvc.perform(post("/api/auth/login").contentType("application/json")
            .content(json.writeValueAsString(Map.of("email","ACC-FLOW-42","password","FlowSecret42!"))))
            .andExpect(status().isUnauthorized());
    }
}

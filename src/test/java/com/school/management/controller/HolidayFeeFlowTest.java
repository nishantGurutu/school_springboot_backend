package com.school.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.user.Role;
import com.school.management.domain.student.Gender;
import com.school.management.entity.*;
import com.school.management.repository.*;
import com.school.management.security.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class HolidayFeeFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @Autowired SectionRepository sections;
    @Autowired SchoolClassRepository classes;
    @Autowired StudentRepository students;
    @Autowired FeesTypeRepository types;
    @Autowired FeesDiscountRepository discounts;
    @Autowired FeePaymentMethodRepository payments;
    @Autowired FeeCollectionRepository collections;
    String accountant;
    FeesTypeEntity type;
    FeesDiscountEntity discount;
    @BeforeEach void setup() {
        accountant = token(Role.ACCOUNTANT);
        var a = sections.save(SectionEntity.builder().name("FLOW-A").build());
        var b = sections.save(SectionEntity.builder().name("FLOW-B").build());
        classes.save(SchoolClassEntity.builder().name("Fee Flow 7").sectionId(a.getId()).section(a.getName()).build());
        classes.save(SchoolClassEntity.builder().name("Fee Flow 8").sectionId(b.getId()).section(b.getName()).build());
        students.save(StudentEntity.builder().admissionNo("FEE-FLOW-001").name("Real Fee Student")
            .firstName("Real").lastName("Student").className("Fee Flow 7").section(a.getName()).rollNo("12")
            .gender(Gender.MALE).dateOfBirth(LocalDate.of(2014,1,1)).phone("1234567890").build());
        type = types.save(FeesTypeEntity.builder().name("Flow Tuition").build());
        discount = discounts.save(FeesDiscountEntity.builder().name("Flow Merit").feesType(type.getName())
            .discountType("Percentage").discountValue("10%").build());
        payments.save(FeePaymentMethodEntity.builder().name("Flow Bank").build());
    }
    String token(Role role) {
        var user = users.save(UserEntity.builder().email("fee.holiday."+role+"@test.local").name("Flow User").password("unused").role(role).build());
        return jwt.generateAccessToken(user.getEmail());
    }
    Map<String,Object> payload() {
        return new HashMap<>(Map.of("className","Fee Flow 7","section","FLOW-A","rollNo","12",
            "feeTypeId",type.getId(),"discountId",discount.getId(),"paymentType","Flow Bank",
            "date","2026-10-28","amount","1500","note","Actual fee"));
    }
    @Test void onlyAdminRolesCanCreateUpdateDeleteHolidays() throws Exception {
        var body = json.writeValueAsString(Map.of("title","Flow Holiday","date","2026-12-25"));
        for (Role role : List.of(Role.ADMIN,Role.MASTER_ADMIN,Role.SUPER_ADMIN)) {
            var auth = "Bearer "+token(role);
            String result = mvc.perform(post("/api/holidays").header("Authorization",auth).contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            long id = json.readTree(result).get("id").asLong();
            mvc.perform(put("/api/holidays/"+id).header("Authorization",auth).contentType("application/json").content(body)).andExpect(status().isOk());
            mvc.perform(delete("/api/holidays/"+id).header("Authorization",auth)).andExpect(status().isNoContent());
        }
    }
    @Test void otherRolesCanReadButCannotMutateHolidays() throws Exception {
        String body = json.writeValueAsString(Map.of("title","Forbidden","date","2026-12-25"));
        for (Role role : List.of(Role.ACCOUNTANT,Role.TEACHER,Role.PRINCIPAL,Role.STUDENT,Role.PARENT,Role.LIBRARIAN,Role.STAFF,Role.DRIVER)) {
            String auth = "Bearer "+(role == Role.ACCOUNTANT ? accountant : token(role));
            mvc.perform(get("/api/holidays").header("Authorization",auth)).andExpect(status().isOk());
            mvc.perform(post("/api/holidays").header("Authorization",auth).contentType("application/json").content(body)).andExpect(status().isForbidden());
            mvc.perform(put("/api/holidays/999").header("Authorization",auth).contentType("application/json").content(body)).andExpect(status().isForbidden());
            mvc.perform(delete("/api/holidays/999").header("Authorization",auth)).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/holidays").contentType("application/json").content(body)).andExpect(status().isUnauthorized());
    }
    @Test void accountantCanReadApiOptionsAndOnlySelectedClassSections() throws Exception {
        String auth = "Bearer "+accountant;
        mvc.perform(get("/api/classes/classes").header("Authorization",auth)).andExpect(status().isOk());
        mvc.perform(get("/api/classes/sections").param("className","Fee Flow 7").header("Authorization",auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].name").value("FLOW-A"));
        mvc.perform(get("/api/classes/sections").param("className","Missing class").header("Authorization",auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/fees/payment-types").header("Authorization",auth)).andExpect(status().isOk());
        mvc.perform(get("/api/fees/types").header("Authorization",auth)).andExpect(status().isOk());
        mvc.perform(get("/api/fees/discounts").header("Authorization",auth)).andExpect(status().isOk());
        mvc.perform(post("/api/classes/sections").header("Authorization",auth).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
    }
    @Test void collectionPersistsRealStudentSelectionsAndAppliesDiscountOnCreateAndEdit() throws Exception {
        var data = payload();
        data.put("name","FAKE NAME"); data.put("admissionNo","FAKE-ID");
        String result = mvc.perform(post("/api/fees/collections").header("Authorization","Bearer "+accountant)
            .contentType("application/json").content(json.writeValueAsString(data)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Real Fee Student"))
            .andExpect(jsonPath("$.admissionNo").value("FEE-FLOW-001"))
            .andExpect(jsonPath("$.feeType").value(type.getName())).andExpect(jsonPath("$.section").value("FLOW-A"))
            .andExpect(jsonPath("$.discountAmount").value("150.00")).andExpect(jsonPath("$.paid").value("1350.00"))
            .andExpect(jsonPath("$.paymentType").value("Flow Bank")).andReturn().getResponse().getContentAsString();
        long id = json.readTree(result).get("id").asLong();
        data.remove("discountId"); data.put("amount","2000");
        mvc.perform(put("/api/fees/collections/"+id).header("Authorization","Bearer "+accountant)
            .contentType("application/json").content(json.writeValueAsString(data)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.paid").value("2000.00"))
            .andExpect(jsonPath("$.discountAmount").value("0.00")).andExpect(jsonPath("$.discountId").isEmpty());
        assertEquals(type.getId(), collections.findById(id).orElseThrow().getFeeTypeId());
    }
    @Test void rejectsInvalidClassSectionStudentPaymentAndInactiveFeeType() throws Exception {
        for (var change : List.of(Map.entry("section","FLOW-B"),Map.entry("rollNo","999"),
                Map.entry("paymentType","Unconfigured"),Map.entry("amount","-10"),Map.entry("date","invalid"))) {
            var data = payload(); data.put(change.getKey(),change.getValue());
            mvc.perform(post("/api/fees/collections").header("Authorization","Bearer "+accountant)
                .contentType("application/json").content(json.writeValueAsString(data))).andExpect(status().isBadRequest());
        }
        type.setStatus("Inactive"); types.save(type);
        mvc.perform(post("/api/fees/collections").header("Authorization","Bearer "+accountant)
            .contentType("application/json").content(json.writeValueAsString(payload()))).andExpect(status().isBadRequest());
    }
    @Test void fixedDiscountAndInvalidDiscountScopesAreValidated() throws Exception {
        discount.setDiscountType("Fixed Amount"); discount.setDiscountValue("200"); discounts.save(discount);
        mvc.perform(post("/api/fees/collections").header("Authorization","Bearer "+accountant)
            .contentType("application/json").content(json.writeValueAsString(payload())))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.paid").value("1300.00"));
        discount.setFeesType("Other Fee"); discounts.save(discount);
        mvc.perform(post("/api/fees/collections").header("Authorization","Bearer "+accountant)
            .contentType("application/json").content(json.writeValueAsString(payload()))).andExpect(status().isBadRequest());
    }
    @Test void createdDiscountReturnsFullDetailsAndAppliesToAnyFeeTypeWhenScopeIsAll() throws Exception {
        var data = Map.of("name","Sibling Discount API", "discountType","Percentage", "discountValue","10%", "status","Active");
        String created = mvc.perform(post("/api/fees/discounts").header("Authorization","Bearer "+accountant)
            .contentType("application/json").content(json.writeValueAsString(data)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.feesType").value("All"))
            .andExpect(jsonPath("$.discountType").value("Percentage")).andExpect(jsonPath("$.discountValue").value("10"))
            .andReturn().getResponse().getContentAsString();
        long id=json.readTree(created).get("id").asLong();
        var collection=payload(); collection.put("discountId",id);
        mvc.perform(post("/api/fees/collections").header("Authorization","Bearer "+accountant)
            .contentType("application/json").content(json.writeValueAsString(collection)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.discount").value("Sibling Discount API"))
            .andExpect(jsonPath("$.paid").value("1350.00"));
        mvc.perform(put("/api/fees/discounts/"+id).header("Authorization","Bearer "+accountant)
            .contentType("application/json").content(json.writeValueAsString(Map.of("name","Sibling Discount API", "feesType",type.getName(),
                "discountType","Fixed Amount","discountValue","200","status","Inactive"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.feesType").value(type.getName()))
            .andExpect(jsonPath("$.discountValue").value("200")).andExpect(jsonPath("$.status").value("Inactive"));
    }
    @Test void legacyHiddenDiscountScopeIsMigratedWithoutChangingNameOrValue() throws Exception {
        discount.setFeesType("May month fees, Admission fees, Exam fees"); discounts.save(discount);
        new com.school.management.config.FeesDiscountScopeMigration(discounts).run(null);
        var migrated=discounts.findById(discount.getId()).orElseThrow();
        assertEquals("All",migrated.getFeesType()); assertEquals("Flow Merit",migrated.getName()); assertEquals("10%",migrated.getDiscountValue());
        var collection=payload();
        mvc.perform(post("/api/fees/collections").header("Authorization","Bearer "+accountant)
            .contentType("application/json").content(json.writeValueAsString(collection)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.paid").value("1350.00"));
    }
    @Test void invalidDiscountValuesAndUnknownFeeScopesAreRejected() throws Exception {
        for(String value:List.of("-1","101","bad","")) {
            mvc.perform(post("/api/fees/discounts").header("Authorization","Bearer "+accountant)
                .contentType("application/json").content(json.writeValueAsString(Map.of("name","Invalid Discount","feesType","All",
                "discountType","Percentage","discountValue",value,"status","Active")))).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/fees/discounts").header("Authorization","Bearer "+accountant)
            .contentType("application/json").content(json.writeValueAsString(Map.of("name","Invalid Scope","feesType","Missing Fee",
                "discountType","Fixed Amount","discountValue","10","status","Active")))).andExpect(status().isBadRequest());
    }

    @Test void accountantAddsPaymentTypeWhichIsListedAndAcceptedForCollection() throws Exception {
        mvc.perform(post("/api/fees/payment-types").header("Authorization","Bearer "+accountant)
            .contentType("application/json").content("{\"name\":\"  Demand   Draft  \"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Demand Draft"))
            .andExpect(jsonPath("$.active").value(true));
        String list = mvc.perform(get("/api/fees/payment-types").header("Authorization","Bearer "+accountant))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(list.contains("Demand Draft"));
        var data=payload(); data.put("paymentType","Demand Draft");
        mvc.perform(post("/api/fees/collections").header("Authorization","Bearer "+accountant)
            .contentType("application/json").content(json.writeValueAsString(data)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.paymentType").value("Demand Draft"));
        mvc.perform(post("/api/fees/payment-types").header("Authorization","Bearer "+accountant)
            .contentType("application/json").content("{\"name\":\"demand draft\"}"))
            .andExpect(status().isConflict());
    }
    @Test void invalidPaymentTypesAreRejectedAndStudentsParentsTeachersCannotAddThem() throws Exception {
        for(String name:List.of("   ","A".repeat(33))) {
            mvc.perform(post("/api/fees/payment-types").header("Authorization","Bearer "+accountant)
                .contentType("application/json").content(json.writeValueAsString(Map.of("name",name))))
                .andExpect(status().isBadRequest());
        }
        for(Role role:List.of(Role.STUDENT,Role.PARENT,Role.TEACHER)) {
            mvc.perform(post("/api/fees/payment-types").header("Authorization","Bearer "+token(role))
                .contentType("application/json").content("{\"name\":\"Forbidden Method\"}"))
                .andExpect(status().isForbidden());
        }
    }

    @Test void adminAndPrincipalPaymentTypesCanBeCreatedAndRead() throws Exception {
        for(Role role:List.of(Role.ADMIN,Role.MASTER_ADMIN,Role.SUPER_ADMIN,Role.PRINCIPAL)) {
            String auth="Bearer "+token(role);
            mvc.perform(post("/api/fees/payment-types").header("Authorization",auth)
                .contentType("application/json").content(json.writeValueAsString(Map.of("name","Method "+role))))
                .andExpect(status().isCreated());
            mvc.perform(get("/api/fees/payment-types").header("Authorization",auth)).andExpect(status().isOk());
        }
    }

}

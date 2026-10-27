package com.school.management.controller;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest @AutoConfigureMockMvc
class DashboardCorsTest {
    @Autowired MockMvc mvc;
    private static final String ORIGIN="https://school-dashboard-one-sepia.vercel.app";
    @Test void vercelLoginPreflightSucceedsWithoutAuthentication() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin",ORIGIN)
                .header("Access-Control-Request-Method","POST")
                .header("Access-Control-Request-Headers","content-type,authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin",ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Methods",containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Credentials","true"));
    }
    @Test void unauthorizedApiResponseStillIncludesCorsHeaders() throws Exception {
        mvc.perform(get("/api/student-reports").header("Origin",ORIGIN))
                .andExpect(status().isUnauthorized()).andExpect(header().string("Access-Control-Allow-Origin",ORIGIN));
    }
    @Test void unrelatedOriginIsRejected() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin","https://unrelated.example")
                .header("Access-Control-Request-Method","POST"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
    @Test void localDevelopmentRemainsAllowed() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin","http://localhost:3000")
                .header("Access-Control-Request-Method","POST"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:3000"));
    }
}

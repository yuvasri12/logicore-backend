package com.devbridge.logicore.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/auth/me tells the logged-in user who they are and where they work. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:logicore_profile_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class ProfileApiTest {

    private static final String PASSWORD = "ChangeMe@123";
    private static final String ADMIN = "yuvasris531@gmail.com";
    private static final String OPS_SOUTH = "priya.nair@logicore.com";
    private static final String STAFF_CHENNAI = "karthik.raja@logicore.com";

    @Autowired
    private MockMvc mockMvc;

    private String token(String email) throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    @Test
    void warehouseStaffSeeTheirWarehouse() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token(STAFF_CHENNAI)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(STAFF_CHENNAI))
                .andExpect(jsonPath("$.role").value("WAREHOUSE_STAFF"))
                .andExpect(jsonPath("$.warehouseName").value("Chennai Hub"))
                .andExpect(jsonPath("$.regionName", nullValue()));
    }

    @Test
    void opsManagerSeesTheirRegion() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token(OPS_SOUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionName").value("South India"))
                .andExpect(jsonPath("$.warehouseName", nullValue()));
    }

    @Test
    void adminHasNoSpecificLocation() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.regionName", nullValue()))
                .andExpect(jsonPath("$.warehouseName", nullValue()));
    }

    @Test
    void meNeedsALogin() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
}
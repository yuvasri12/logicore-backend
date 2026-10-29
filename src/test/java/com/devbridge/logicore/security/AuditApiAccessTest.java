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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:logicore_audit_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class AuditApiAccessTest {

    private static final String PASSWORD = "ChangeMe@123";
    private static final String ADMIN = "yuvasris531@gmail.com";
    private static final String OPS = "priya.nair@logicore.com";
    private static final String WAREHOUSE = "karthik.raja@logicore.com";

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
    void noToken_isRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/audit"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void opsManager_isForbidden() throws Exception {
        mockMvc.perform(get("/api/audit").header("Authorization", "Bearer " + token(OPS)))
                .andExpect(status().isForbidden());
    }

    @Test
    void warehouseStaff_isForbidden() throws Exception {
        mockMvc.perform(get("/api/audit").header("Authorization", "Bearer " + token(WAREHOUSE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_seesNewestEntryFirst_andPagingIsApplied() throws Exception {
        String body = "{\"name\":\"Audit Test Co\",\"contactPerson\":\"Asha\",\"email\":\"a@audit.com\","
                + "\"phone\":\"+91 90000 00000\",\"address\":\"Chennai\"}";
        mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + token(OPS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/audit?page=0&size=1")
                        .header("Authorization", "Bearer " + token(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].action").value("CUSTOMER_CREATED"))
                .andExpect(jsonPath("$.items[0].entityType").value("CUSTOMER"))
                .andExpect(jsonPath("$.size").value(1));
    }
}
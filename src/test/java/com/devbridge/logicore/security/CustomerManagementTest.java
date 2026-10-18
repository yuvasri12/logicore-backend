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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:logicore_customers_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class CustomerManagementTest {

    private static final String PASSWORD = "ChangeMe@123";

    @Autowired
    private MockMvc mockMvc;

    private String token(String email) throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private MvcResult createCustomer(String token, String name) throws Exception {
        String body = String.format("{\"name\":\"%s\",\"contactPerson\":\"Asha\",\"email\":\"ops@acme.com\","
                + "\"phone\":\"+91 90000 00000\",\"address\":\"Chennai\"}", name);
        return mockMvc.perform(post("/api/customers")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)).andReturn();
    }

    @Test
    void onlyAdminAndOpsManagerCanUseCustomers() throws Exception {
        mockMvc.perform(get("/api/customers")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + token("warehouse@logicore.local")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + token("dispatcher@logicore.local")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + token("ops.manager@logicore.local")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + token("yuvasris531@gmail.com")))
                .andExpect(status().isOk());
    }

    @Test
    void createCustomer_duplicateNameIsRejected_blankNameIsRejected() throws Exception {
        String ops = token("ops.manager@logicore.local");

        MvcResult created = createCustomer(ops, "Acme Traders");
        org.junit.jupiter.api.Assertions.assertEquals(201, created.getResponse().getStatus());

        MvcResult duplicate = createCustomer(ops, "ACME traders");
        org.junit.jupiter.api.Assertions.assertEquals(409, duplicate.getResponse().getStatus());

        mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + ops)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAndDeactivateAndActivateCustomer() throws Exception {
        String admin = token("yuvasris531@gmail.com");
        MvcResult created = createCustomer(admin, "Globex Freight");
        Integer id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/customers/" + id)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Globex Freight Ltd\",\"contactPerson\":\"Ravi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Globex Freight Ltd"))
                .andExpect(jsonPath("$.contactPerson").value("Ravi"));

        mockMvc.perform(post("/api/customers/" + id + "/deactivate")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/customers/" + id + "/activate")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(put("/api/customers/999999")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nobody\"}"))
                .andExpect(status().isNotFound());
    }
}
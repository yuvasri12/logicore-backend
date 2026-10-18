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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:logicore_users_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class UserManagementTest {

    private static final String PASSWORD = "ChangeMe@123";

    @Autowired
    private MockMvc mockMvc;

    private MvcResult login(String email, String password) throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)).andReturn();
    }

    private String token(String email) throws Exception {
        MvcResult result = login(email, PASSWORD);
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String adminToken() throws Exception {
        return token("yuvasris531@gmail.com");
    }

    private MvcResult createUser(String adminToken, String email, String role) throws Exception {
        String body = String.format(
                "{\"fullName\":\"Test User\",\"email\":\"%s\",\"role\":\"%s\",\"password\":\"%s\"}",
                email, role, PASSWORD);
        return mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)).andReturn();
    }

    @Test
    void onlyAdminCanListUsers() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token("dispatcher@logicore.local")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").exists());
    }

    @Test
    void adminCreatesUser_duplicateIsRejected_andNewUserCanLogIn() throws Exception {
        String admin = adminToken();

        MvcResult created = createUser(admin, "new.user@logicore.local", "WAREHOUSE_STAFF");
        org.junit.jupiter.api.Assertions.assertEquals(201, created.getResponse().getStatus());

        MvcResult duplicate = createUser(admin, "NEW.user@logicore.local", "DISPATCHER");
        org.junit.jupiter.api.Assertions.assertEquals(409, duplicate.getResponse().getStatus());

        org.junit.jupiter.api.Assertions.assertEquals(200,
                login("new.user@logicore.local", PASSWORD).getResponse().getStatus());
    }

    @Test
    void shortPasswordIsRejected() throws Exception {
        String body = "{\"fullName\":\"Short Pw\",\"email\":\"short.pw@logicore.local\","
                + "\"role\":\"DISPATCHER\",\"password\":\"abc\"}";
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCannotChangeOwnRoleOrDisableSelf() throws Exception {
        MvcResult result = login("yuvasris531@gmail.com", PASSWORD);
        String json = result.getResponse().getContentAsString();
        String token = JsonPath.read(json, "$.accessToken");
        Integer myId = JsonPath.read(json, "$.user.id");

        mockMvc.perform(patch("/api/users/" + myId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\",\"enabled\":false}"))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/users/" + myId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"DISPATCHER\",\"enabled\":true}"))
                .andExpect(status().isConflict());
    }

    @Test
    void adminCanChangeRoleAndDisableAnotherUser() throws Exception {
        String admin = adminToken();
        MvcResult created = createUser(admin, "changeme.user@logicore.local", "DISPATCHER");
        Integer id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(patch("/api/users/" + id)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"WAREHOUSE_STAFF\",\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("WAREHOUSE_STAFF"));

        mockMvc.perform(patch("/api/users/" + id)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"WAREHOUSE_STAFF\",\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        // A disabled user can no longer log in.
        org.junit.jupiter.api.Assertions.assertEquals(401,
                login("changeme.user@logicore.local", PASSWORD).getResponse().getStatus());
    }
}
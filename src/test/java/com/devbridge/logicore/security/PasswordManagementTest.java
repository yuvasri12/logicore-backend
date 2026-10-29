package com.devbridge.logicore.security;

import com.devbridge.logicore.repository.WarehouseRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:logicore_password_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class PasswordManagementTest {

    private static final String PASSWORD = "ChangeMe@123";
    private static final String NEW_PASSWORD = "NewPass@456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WarehouseRepository warehouses;

    private MvcResult login(String email, String password) throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
    }

    private int loginStatus(String email, String password) throws Exception {
        return login(email, password).getResponse().getStatus();
    }

    private String tokenOf(String email, String password) throws Exception {
        return JsonPath.read(login(email, password).getResponse().getContentAsString(), "$.accessToken");
    }

    // The roles used here (Dispatcher, Warehouse Staff) must belong to a warehouse.
    private Integer createUser(String email, String role) throws Exception {
        Long chennai = warehouses.findByCodeIgnoreCase("CHN-01").get().getId();
        String body = String.format(
                "{\"fullName\":\"Test User\",\"email\":\"%s\",\"role\":\"%s\",\"password\":\"%s\",\"warehouseId\":%d}",
                email, role, PASSWORD, chennai);
        MvcResult result = mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + tokenOf("yuvasris531@gmail.com", PASSWORD))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private MvcResult changePassword(String token, String current, String next) throws Exception {
        String body = String.format("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}", current, next);
        return mockMvc.perform(post("/api/auth/change-password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
    }

    private MvcResult resetPassword(String token, Integer id, String next) throws Exception {
        return mockMvc.perform(post("/api/users/" + id + "/reset-password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\":\"" + next + "\"}")).andReturn();
    }

    @Test
    void userChangesOwnPassword_oldStopsWorking_newWorks() throws Exception {
        createUser("pw.change@logicore.local", "DISPATCHER");
        String token = tokenOf("pw.change@logicore.local", PASSWORD);

        assertEquals(204, changePassword(token, PASSWORD, NEW_PASSWORD).getResponse().getStatus());
        assertEquals(401, loginStatus("pw.change@logicore.local", PASSWORD));
        assertEquals(200, loginStatus("pw.change@logicore.local", NEW_PASSWORD));
    }

    @Test
    void wrongCurrentPassword_isRejectedWithClearMessage_andNothingChanges() throws Exception {
        createUser("pw.wrong@logicore.local", "DISPATCHER");
        String token = tokenOf("pw.wrong@logicore.local", PASSWORD);

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Not.The.Password1\",\"newPassword\":\"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Your current password is incorrect."));

        assertEquals(200, loginStatus("pw.wrong@logicore.local", PASSWORD));
    }

    @Test
    void newPasswordSameAsCurrent_isRejected() throws Exception {
        createUser("pw.same@logicore.local", "DISPATCHER");
        String token = tokenOf("pw.same@logicore.local", PASSWORD);
        assertEquals(400, changePassword(token, PASSWORD, PASSWORD).getResponse().getStatus());
    }

    @Test
    void shortNewPassword_isRejected() throws Exception {
        createUser("pw.short@logicore.local", "DISPATCHER");
        String token = tokenOf("pw.short@logicore.local", PASSWORD);
        assertEquals(400, changePassword(token, PASSWORD, "abc").getResponse().getStatus());
    }

    @Test
    void changePasswordNeedsLogin() throws Exception {
        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"x\",\"newPassword\":\"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminResetsAnotherUsersPassword() throws Exception {
        Integer id = createUser("pw.reset@logicore.local", "WAREHOUSE_STAFF");
        String admin = tokenOf("yuvasris531@gmail.com", PASSWORD);

        assertEquals(204, resetPassword(admin, id, "Reset@789ab").getResponse().getStatus());
        assertEquals(401, loginStatus("pw.reset@logicore.local", PASSWORD));
        assertEquals(200, loginStatus("pw.reset@logicore.local", "Reset@789ab"));
    }

    @Test
    void nonAdminCannotResetPasswords() throws Exception {
        Integer id = createUser("pw.target@logicore.local", "WAREHOUSE_STAFF");
        String dispatcher = tokenOf("meena.s@logicore.com", PASSWORD);
        assertEquals(403, resetPassword(dispatcher, id, "Reset@789ab").getResponse().getStatus());
    }

    @Test
    void adminCannotResetOwnPassword() throws Exception {
        MvcResult result = login("yuvasris531@gmail.com", PASSWORD);
        String json = result.getResponse().getContentAsString();
        String token = JsonPath.read(json, "$.accessToken");
        Integer myId = JsonPath.read(json, "$.user.id");
        assertEquals(409, resetPassword(token, myId, "Reset@789ab").getResponse().getStatus());
    }
}
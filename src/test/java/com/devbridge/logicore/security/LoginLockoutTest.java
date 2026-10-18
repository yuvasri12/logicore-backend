package com.devbridge.logicore.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:logicore_lock_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "app.security.login.max-attempts=5",
        "app.security.login.lock-minutes=10"
})
class LoginLockoutTest {

    private static final String GOOD_PASSWORD = "ChangeMe@123";

    @Autowired
    private MockMvc mockMvc;

    private ResultActions login(String email, String password) throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void locksAfterFiveWrongPasswords_evenForTheCorrectPassword() throws Exception {
        String email = "warehouse@logicore.local";

        for (int i = 0; i < 5; i++) {
            login(email, "wrong-password").andExpect(status().isUnauthorized());
        }

        // 6th try: locked, even though the password is now correct.
        login(email, GOOD_PASSWORD).andExpect(status().isTooManyRequests());
    }

    @Test
    void successfulLoginResetsTheCounter() throws Exception {
        String email = "dispatcher@logicore.local";

        for (int i = 0; i < 4; i++) {
            login(email, "wrong-password").andExpect(status().isUnauthorized());
        }
        login(email, GOOD_PASSWORD).andExpect(status().isOk());

        // Counter was reset, so 4 more wrong tries are still just 401.
        for (int i = 0; i < 4; i++) {
            login(email, "wrong-password").andExpect(status().isUnauthorized());
        }
        login(email, GOOD_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void unknownEmailIsLockedToo_soNobodyCanTellWhichEmailsExist() throws Exception {
        String email = "nobody@logicore.local";

        for (int i = 0; i < 5; i++) {
            login(email, "wrong-password").andExpect(status().isUnauthorized());
        }
        login(email, "wrong-password").andExpect(status().isTooManyRequests());
    }
}
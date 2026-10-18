package com.devbridge.logicore.security;

import com.devbridge.logicore.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Checks the role table end to end: for every endpoint, each role must get 403 when it is not
 * allowed, and must get past security (anything other than 401/403) when it is allowed.
 * Uses an in-memory H2 database, so the real PostgreSQL data is not touched.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:logicore_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class RoleAccessTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    private String bearer(Role role) {
        String token = jwtService.generate(1L, role.name().toLowerCase() + "@test.local", "Test " + role.name(), role);
        return "Bearer " + token;
    }

    /** Sends the request and returns the HTTP status. A null role means "not logged in". */
    private int call(HttpMethod method, String url, String authorizationHeader) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(method, url)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");
        if (authorizationHeader != null) {
            request.header("Authorization", authorizationHeader);
        }
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    /** Not logged in => 401. Each role => 403 if not allowed, otherwise past security. */
    private void expectRoles(HttpMethod method, String url, Role... allowed) throws Exception {
        List<Role> allowedRoles = Arrays.asList(allowed);

        assertEquals(401, call(method, url, null), "not logged in: " + method + " " + url);

        for (Role role : Role.values()) {
            int status = call(method, url, bearer(role));
            String label = role + " on " + method + " " + url + " (status " + status + ")";
            if (allowedRoles.contains(role)) {
                assertNotEquals(401, status, label + " should be allowed");
                assertNotEquals(403, status, label + " should be allowed");
            } else {
                assertEquals(403, status, label + " should be denied");
            }
        }
    }

    @Test
    void anyLoggedInUserCanReadShipmentsAndOwnProfile() throws Exception {
        expectRoles(HttpMethod.GET, "/api/shipments", Role.values());
        expectRoles(HttpMethod.GET, "/api/auth/me", Role.values());
    }

    @Test
    void createEditAndCancelShipmentAreForAdminAndOps() throws Exception {
        expectRoles(HttpMethod.POST, "/api/shipments", Role.ADMIN, Role.OPS_MANAGER);
        expectRoles(HttpMethod.PUT, "/api/shipments/1", Role.ADMIN, Role.OPS_MANAGER);
        expectRoles(HttpMethod.DELETE, "/api/shipments/1", Role.ADMIN, Role.OPS_MANAGER);
    }

    @Test
    void directStatusChangeIsAdminOnly() throws Exception {
        expectRoles(HttpMethod.PATCH, "/api/shipments/1/status", Role.ADMIN);
    }

    @Test
    void loadingIsForWarehouseSide() throws Exception {
        expectRoles(HttpMethod.POST, "/api/shipments/1/load", Role.ADMIN, Role.OPS_MANAGER, Role.WAREHOUSE_STAFF);
    }

    @Test
    void deliveringIsForDispatchSide() throws Exception {
        expectRoles(HttpMethod.POST, "/api/shipments/1/deliver", Role.ADMIN, Role.OPS_MANAGER, Role.DISPATCHER);
    }

    @Test
    void warehouseActionsAreForWarehouseSide() throws Exception {
        expectRoles(HttpMethod.POST, "/api/warehouse/receive", Role.ADMIN, Role.OPS_MANAGER, Role.WAREHOUSE_STAFF);
        expectRoles(HttpMethod.POST, "/api/warehouse/assign-location", Role.ADMIN, Role.OPS_MANAGER, Role.WAREHOUSE_STAFF);
    }

    @Test
    void dispatchIsForDispatchSide() throws Exception {
        expectRoles(HttpMethod.POST, "/api/dispatch", Role.ADMIN, Role.OPS_MANAGER, Role.DISPATCHER);
    }

    @Test
    void trackingAndLoginArePublic() throws Exception {
        int tracking = call(HttpMethod.GET, "/api/tracking/DOES-NOT-EXIST", null);
        assertNotEquals(401, tracking, "public tracking must not ask for a login");
        assertNotEquals(403, tracking, "public tracking must not be forbidden");

        int login = call(HttpMethod.POST, "/api/auth/login", null);
        assertNotEquals(401, login, "login must not ask for a login");
        assertNotEquals(403, login, "login must not be forbidden");
    }

    @Test
    void garbageTokenIsRejected() throws Exception {
        assertEquals(401, call(HttpMethod.GET, "/api/shipments", "Bearer not-a-real-token"));
    }
}
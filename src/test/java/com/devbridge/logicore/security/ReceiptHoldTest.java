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
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Freight received with a problem is on hold until an Ops Manager or Admin clears it. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:logicore_hold_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class ReceiptHoldTest {

    private static final String PASSWORD = "ChangeMe@123";
    private static final String ADMIN = "yuvasris531@gmail.com";
    private static final String OPS_SOUTH = "priya.nair@logicore.com";
    private static final String OPS_UAE = "omar.haddad@logicore.com";
    private static final String STAFF_CHENNAI = "karthik.raja@logicore.com";
    private static final String DISPATCHER_CHENNAI = "meena.s@logicore.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WarehouseRepository warehouses;

    private String token(String email) throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    /** A shipment that starts and ends at the Chennai hub. */
    private Integer createShipment(String opsToken) throws Exception {
        Long id = warehouses.findByCodeIgnoreCase("CHN-01").get().getId();
        String body = "{\"customerName\":\"Hold Test\",\"origin\":\"A\",\"destination\":\"B\","
                + "\"originWarehouseId\":" + id + ",\"destinationWarehouseId\":" + id + ","
                + "\"mode\":\"AIR\",\"weight\":10}";
        MvcResult result = mockMvc.perform(post("/api/shipments")
                        .header("Authorization", "Bearer " + opsToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is2xxSuccessful()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    /** Receives with the given condition and puts the freight on a rack. */
    private void receiveAndAssign(String staffToken, Integer id, String condition) throws Exception {
        String receive = "{\"shipmentId\":" + id + ",\"condition\":\"" + condition + "\","
                + "\"weightChecked\":10,\"receivedBy\":\"x\"}";
        mockMvc.perform(post("/api/warehouse/receive")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON).content(receive))
                .andExpect(status().is2xxSuccessful());

        String assign = "{\"shipmentId\":" + id + ",\"zone\":\"H\",\"rack\":\"1\",\"bin\":\"1\",\"assignedBy\":\"x\"}";
        mockMvc.perform(post("/api/warehouse/assign-location")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON).content(assign))
                .andExpect(status().is2xxSuccessful());
    }

    private ResultActions load(String token, Integer id) throws Exception {
        return mockMvc.perform(post("/api/shipments/" + id + "/load")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"updatedBy\":\"x\",\"remarks\":\"ok\"}"));
    }

    private ResultActions dispatch(String token, Integer id) throws Exception {
        return mockMvc.perform(post("/api/dispatch")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"shipmentId\":" + id + ",\"vehicleNumber\":\"TN01AB1234\","
                        + "\"destination\":\"Bengaluru\",\"dispatchedBy\":\"x\"}"));
    }

    private ResultActions clear(String token, Integer id, String note) throws Exception {
        return mockMvc.perform(post("/api/warehouse/shipments/" + id + "/clear-issue")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"" + note + "\"}"));
    }

    @Test
    void damagedFreightCannotBeLoadedUntilAManagerClearsIt() throws Exception {
        String ops = token(OPS_SOUTH);
        String staff = token(STAFF_CHENNAI);
        Integer id = createShipment(ops);
        receiveAndAssign(staff, id, "DAMAGED");

        load(staff, id).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FREIGHT_ON_HOLD"));

        // Warehouse staff cannot clear it themselves.
        clear(staff, id, "Box dented, contents fine").andExpect(status().isForbidden());

        clear(ops, id, "Box dented, contents fine").andExpect(status().isNoContent());
        load(staff, id).andExpect(status().is2xxSuccessful());
    }

    @Test
    void dispatchIsAlsoBlockedWhileOnHold() throws Exception {
        String ops = token(OPS_SOUTH);
        Integer id = createShipment(ops);
        receiveAndAssign(token(STAFF_CHENNAI), id, "MISSING_ITEMS");

        String dispatcher = token(DISPATCHER_CHENNAI);
        dispatch(dispatcher, id).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FREIGHT_ON_HOLD"));

        clear(ops, id, "Customer confirmed the missing item").andExpect(status().isNoContent());
        dispatch(dispatcher, id).andExpect(status().is2xxSuccessful());
    }

    @Test
    void adminStatusUpdateCannotBypassTheHold() throws Exception {
        Integer id = createShipment(token(OPS_SOUTH));
        receiveAndAssign(token(STAFF_CHENNAI), id, "WEIGHT_MISMATCH");

        mockMvc.perform(patch("/api/shipments/" + id + "/status")
                        .header("Authorization", "Bearer " + token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LOADED\",\"remarks\":\"r\",\"updatedBy\":\"x\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FREIGHT_ON_HOLD"));
    }

    @Test
    void goodFreightIsNeverOnHold() throws Exception {
        Integer id = createShipment(token(OPS_SOUTH));
        receiveAndAssign(token(STAFF_CHENNAI), id, "GOOD");

        load(token(STAFF_CHENNAI), id).andExpect(status().is2xxSuccessful());
    }

    @Test
    void clearingNeedsAnOpenIssue_aNote_andYourOwnScope() throws Exception {
        String ops = token(OPS_SOUTH);
        Integer good = createShipment(ops);
        receiveAndAssign(token(STAFF_CHENNAI), good, "GOOD");
        clear(ops, good, "nothing to clear").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_OPEN_ISSUE"));

        Integer damaged = createShipment(ops);
        receiveAndAssign(token(STAFF_CHENNAI), damaged, "DAMAGED");

        // The note is required.
        mockMvc.perform(post("/api/warehouse/shipments/" + damaged + "/clear-issue")
                        .header("Authorization", "Bearer " + ops)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"\"}"))
                .andExpect(status().isBadRequest());

        // A manager from another region cannot even see the shipment.
        clear(token(OPS_UAE), damaged, "not mine").andExpect(status().isNotFound());
    }
}
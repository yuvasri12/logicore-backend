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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A shipment that crosses a country border cannot be loaded or dispatched until customs is cleared. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:logicore_customs_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class CustomsTest {

    private static final String PASSWORD = "ChangeMe@123";
    private static final String ADMIN = "yuvasris531@gmail.com";
    private static final String OPS_SOUTH = "priya.nair@logicore.com";
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

    /** Creates a shipment between two warehouses (by code). */
    private Integer createShipment(String opsToken, String fromCode, String toCode) throws Exception {
        Long from = warehouses.findByCodeIgnoreCase(fromCode).get().getId();
        Long to = warehouses.findByCodeIgnoreCase(toCode).get().getId();
        String body = "{\"customerName\":\"Customs Test\",\"origin\":\"A\",\"destination\":\"B\","
                + "\"originWarehouseId\":" + from + ",\"destinationWarehouseId\":" + to + ","
                + "\"mode\":\"AIR\",\"weight\":10}";
        MvcResult result = mockMvc.perform(post("/api/shipments")
                        .header("Authorization", "Bearer " + opsToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is2xxSuccessful()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private Integer createInternational(String opsToken) throws Exception {
        return createShipment(opsToken, "CHN-01", "DXB-01");
    }

    private Integer createDomestic(String opsToken) throws Exception {
        return createShipment(opsToken, "CHN-01", "CHN-01");
    }

    /** Receives in good condition and puts the freight on a rack, so it is ready to load. */
    private void receiveAndAssign(String staffToken, Integer id) throws Exception {
        String receive = "{\"shipmentId\":" + id + ",\"condition\":\"GOOD\","
                + "\"weightChecked\":10,\"receivedBy\":\"x\"}";
        mockMvc.perform(post("/api/warehouse/receive")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON).content(receive))
                .andExpect(status().is2xxSuccessful());

        String assign = "{\"shipmentId\":" + id + ",\"zone\":\"C\",\"rack\":\"1\",\"bin\":\"1\",\"assignedBy\":\"x\"}";
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
                        + "\"destination\":\"Dubai\",\"dispatchedBy\":\"x\"}"));
    }

    private ResultActions clearCustoms(String token, Integer id, String reference) throws Exception {
        return mockMvc.perform(post("/api/customs/shipments/" + id + "/clear")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customsReference\":\"" + reference + "\",\"note\":\"checked\"}"));
    }

    private ResultActions view(String token, Integer id) throws Exception {
        return mockMvc.perform(get("/api/shipments/" + id)
                .header("Authorization", "Bearer " + token));
    }

    @Test
    void internationalShipmentIsPendingAndCannotBeLoadedOrDispatched() throws Exception {
        String ops = token(OPS_SOUTH);
        Integer id = createInternational(ops);
        receiveAndAssign(token(STAFF_CHENNAI), id);

        view(ops, id).andExpect(status().isOk())
                .andExpect(jsonPath("$.customsStatus").value("PENDING"));

        load(token(STAFF_CHENNAI), id).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CUSTOMS_NOT_CLEARED"));

        dispatch(token(DISPATCHER_CHENNAI), id).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CUSTOMS_NOT_CLEARED"));
    }

    @Test
    void managerClearsCustomsThenLoadingWorks() throws Exception {
        String ops = token(OPS_SOUTH);
        String staff = token(STAFF_CHENNAI);
        Integer id = createInternational(ops);
        receiveAndAssign(staff, id);

        // Warehouse staff cannot clear customs.
        clearCustoms(staff, id, "CUS-100").andExpect(status().isForbidden());

        // The reference is required.
        clearCustoms(ops, id, "").andExpect(status().isBadRequest());

        clearCustoms(ops, id, "CUS-100").andExpect(status().isNoContent());
        view(ops, id).andExpect(jsonPath("$.customsStatus").value("CLEARED"));

        load(staff, id).andExpect(status().is2xxSuccessful());

        // Clearing twice is refused.
        clearCustoms(ops, id, "CUS-101").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ACTION"));
    }

    @Test
    void dispatchWorksAfterCustomsIsCleared() throws Exception {
        String ops = token(OPS_SOUTH);
        Integer id = createInternational(ops);
        receiveAndAssign(token(STAFF_CHENNAI), id);

        clearCustoms(ops, id, "CUS-200").andExpect(status().isNoContent());
        dispatch(token(DISPATCHER_CHENNAI), id).andExpect(status().is2xxSuccessful());
    }

    @Test
    void adminStatusUpdateCannotBypassCustoms() throws Exception {
        Integer id = createInternational(token(OPS_SOUTH));
        receiveAndAssign(token(STAFF_CHENNAI), id);

        mockMvc.perform(patch("/api/shipments/" + id + "/status")
                        .header("Authorization", "Bearer " + token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LOADED\",\"remarks\":\"r\",\"updatedBy\":\"x\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CUSTOMS_NOT_CLEARED"));
    }

    @Test
    void domesticShipmentNeedsNoCustoms() throws Exception {
        String ops = token(OPS_SOUTH);
        Integer id = createDomestic(ops);
        receiveAndAssign(token(STAFF_CHENNAI), id);

        view(ops, id).andExpect(jsonPath("$.customsStatus").value("NOT_REQUIRED"));

        clearCustoms(ops, id, "CUS-300").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CUSTOMS_NOT_REQUIRED"));

        load(token(STAFF_CHENNAI), id).andExpect(status().is2xxSuccessful());
    }
}
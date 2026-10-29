package com.devbridge.logicore.security;

import com.devbridge.logicore.model.Warehouse;
import com.devbridge.logicore.repository.StorageLocationRepository;
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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Storage locations belong to a warehouse: "Rack A1" in Chennai and in Dubai are different places. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:logicore_storage_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class StorageLocationTest {

    private static final String PASSWORD = "ChangeMe@123";
    private static final String OPS_SOUTH = "priya.nair@logicore.com";
    private static final String OPS_UAE = "omar.haddad@logicore.com";
    private static final String STAFF_CHENNAI = "karthik.raja@logicore.com";
    private static final String STAFF_DUBAI = "faisal.khan@logicore.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WarehouseRepository warehouses;

    @Autowired
    private StorageLocationRepository locations;

    private String token(String email) throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private Warehouse warehouse(String code) {
        return warehouses.findByCodeIgnoreCase(code).get();
    }

    /** Creates a shipment that starts and ends at this warehouse. */
    private Integer createShipment(String opsToken, String code) throws Exception {
        Long id = warehouse(code).getId();
        String body = "{\"customerName\":\"Storage Test\",\"origin\":\"A\",\"destination\":\"B\","
                + "\"originWarehouseId\":" + id + ",\"destinationWarehouseId\":" + id + ","
                + "\"mode\":\"AIR\",\"weight\":10}";
        MvcResult result = mockMvc.perform(post("/api/shipments")
                        .header("Authorization", "Bearer " + opsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    /** Receives the shipment, then puts it on the given rack. */
    private void receiveAndAssign(String staffToken, Integer id, String zone, String rack, String bin)
            throws Exception {
        String receive = "{\"shipmentId\":" + id + ",\"condition\":\"GOOD\","
                + "\"weightChecked\":10,\"receivedBy\":\"x\"}";
        mockMvc.perform(post("/api/warehouse/receive")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON).content(receive))
                .andExpect(status().is2xxSuccessful());

        String assign = "{\"shipmentId\":" + id + ",\"zone\":\"" + zone + "\",\"rack\":\"" + rack
                + "\",\"bin\":\"" + bin + "\",\"assignedBy\":\"x\"}";
        mockMvc.perform(post("/api/warehouse/assign-location")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON).content(assign))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void sameRackNameInTwoWarehouses_isTwoSeparateLocations() throws Exception {
        Integer chennai = createShipment(token(OPS_SOUTH), "CHN-01");
        Integer dubai = createShipment(token(OPS_UAE), "DXB-01");

        receiveAndAssign(token(STAFF_CHENNAI), chennai, "T", "1", "1");
        receiveAndAssign(token(STAFF_DUBAI), dubai, "T", "1", "1");

        Long chennaiLocation = locations
                .findByWarehouseAndZoneAndRackAndBin(warehouse("CHN-01"), "T", "1", "1").get().getId();
        Long dubaiLocation = locations
                .findByWarehouseAndZoneAndRackAndBin(warehouse("DXB-01"), "T", "1", "1").get().getId();

        assertNotEquals(chennaiLocation, dubaiLocation);
    }

    @Test
    void sameWarehouseSameRack_isReusedNotDuplicated() throws Exception {
        String ops = token(OPS_UAE);
        String staff = token(STAFF_DUBAI);

        receiveAndAssign(staff, createShipment(ops, "DXB-01"), "U", "2", "2");
        // A second shipment on the same rack must reuse the location (a duplicate would break the unique rule).
        receiveAndAssign(staff, createShipment(ops, "DXB-01"), "U", "2", "2");

        assertTrue(locations
                .findByWarehouseAndZoneAndRackAndBin(warehouse("DXB-01"), "U", "2", "2").isPresent());
    }
}
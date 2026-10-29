package com.devbridge.logicore.security;

import com.devbridge.logicore.repository.RegionRepository;
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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Data scoping: who may see and act on which shipments.
 * The demo data (bootstrap) gives: Priya Nair = South India, Omar Haddad = UAE,
 * Karthik Raja = Chennai hub, Faisal Khan = Dubai hub.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:logicore_scope_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class DataScopingTest {

    private static final String PASSWORD = "ChangeMe@123";
    private static final String ADMIN = "yuvasris531@gmail.com";
    private static final String OPS_SOUTH = "priya.nair@logicore.com";
    private static final String OPS_UAE = "omar.haddad@logicore.com";
    private static final String STAFF_CHENNAI = "karthik.raja@logicore.com";
    private static final String STAFF_DUBAI = "faisal.khan@logicore.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WarehouseRepository warehouses;

    @Autowired
    private RegionRepository regions;

    private String token(String email) throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private Long warehouseId(String code) {
        return warehouses.findByCodeIgnoreCase(code).get().getId();
    }

    private ResultActions createShipment(String token, String originCode, String destinationCode) throws Exception {
        String body = "{\"customerName\":\"Scope Test\",\"origin\":\"A\",\"destination\":\"B\","
                + "\"originWarehouseId\":" + warehouseId(originCode) + ","
                + "\"destinationWarehouseId\":" + warehouseId(destinationCode) + ","
                + "\"mode\":\"AIR\",\"weight\":10}";
        return mockMvc.perform(post("/api/shipments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private Integer createdId(String token, String originCode, String destinationCode) throws Exception {
        MvcResult result = createShipment(token, originCode, destinationCode)
                .andExpect(status().is2xxSuccessful()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private ResultActions getShipment(String token, Integer id) throws Exception {
        return mockMvc.perform(get("/api/shipments/" + id).header("Authorization", "Bearer " + token));
    }

    private ResultActions listShipments(String token) throws Exception {
        return mockMvc.perform(get("/api/shipments").header("Authorization", "Bearer " + token));
    }

    @Test
    void shipmentInDubaiIsInvisibleToSouthIndiaOps_butVisibleToUaeOpsAndAdmin() throws Exception {
        Integer id = createdId(token(OPS_UAE), "DXB-01", "DXB-01");

        getShipment(token(OPS_SOUTH), id).andExpect(status().isNotFound());
        getShipment(token(OPS_UAE), id).andExpect(status().isOk());
        getShipment(token(ADMIN), id).andExpect(status().isOk());

        listShipments(token(OPS_SOUTH)).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(id))));
        listShipments(token(OPS_UAE)).andExpect(jsonPath("$[*].id", hasItem(id)));
    }

    @Test
    void crossRegionShipmentIsVisibleAtBothEnds() throws Exception {
        Integer id = createdId(token(OPS_SOUTH), "CHN-01", "DXB-01");

        getShipment(token(OPS_SOUTH), id).andExpect(status().isOk());
        getShipment(token(OPS_UAE), id).andExpect(status().isOk());
        listShipments(token(OPS_UAE)).andExpect(jsonPath("$[*].id", hasItem(id)));
    }

    @Test
    void warehouseStaffSeeOnlyTheirOwnWarehouse_whileTheRegionManagerSeesBoth() throws Exception {
        Integer id = createdId(token(OPS_SOUTH), "BLR-01", "BLR-01");

        listShipments(token(OPS_SOUTH)).andExpect(jsonPath("$[*].id", hasItem(id)));
        listShipments(token(STAFF_CHENNAI)).andExpect(jsonPath("$[*].id", not(hasItem(id))));
        getShipment(token(STAFF_CHENNAI), id).andExpect(status().isNotFound());
    }

    @Test
    void creatingOutsideYourOwnScopeIsRefused() throws Exception {
        createShipment(token(OPS_SOUTH), "DXB-01", "DXB-01")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));
    }

    @Test
    void warehouseActionOnAnotherWarehousesShipmentIsNotFound() throws Exception {
        Integer id = createdId(token(OPS_UAE), "DXB-01", "DXB-01");
        String receive = "{\"shipmentId\":" + id + ",\"condition\":\"GOOD\","
                + "\"weightChecked\":10,\"receivedBy\":\"x\"}";

        mockMvc.perform(post("/api/warehouse/receive")
                        .header("Authorization", "Bearer " + token(STAFF_CHENNAI))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(receive))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/warehouse/receive")
                        .header("Authorization", "Bearer " + token(STAFF_DUBAI))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(receive))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void organisationApi_adminManages_opsReads_othersDenied() throws Exception {
        mockMvc.perform(get("/api/org/warehouses").header("Authorization", "Bearer " + token(OPS_SOUTH)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/org/warehouses").header("Authorization", "Bearer " + token(STAFF_CHENNAI)))
                .andExpect(status().isForbidden());

        String region = "{\"code\":\"tst\",\"name\":\"Test Region\"}";
        mockMvc.perform(post("/api/org/regions")
                        .header("Authorization", "Bearer " + token(OPS_SOUTH))
                        .contentType(MediaType.APPLICATION_JSON).content(region))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/org/regions")
                        .header("Authorization", "Bearer " + token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(region))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TST"));

        mockMvc.perform(post("/api/org/regions")
                        .header("Authorization", "Bearer " + token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(region))
                .andExpect(status().isConflict());
    }

    @Test
    void newOpsManagerNeedsARegion() throws Exception {
        String admin = token(ADMIN);
        String withoutRegion = "{\"fullName\":\"No Region\",\"email\":\"no.region@logicore.local\","
                + "\"role\":\"OPS_MANAGER\",\"password\":\"" + PASSWORD + "\"}";
        mockMvc.perform(post("/api/users").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(withoutRegion))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ASSIGNMENT_REQUIRED"));

        Long south = regions.findByCodeIgnoreCase("SI").get().getId();
        String withRegion = "{\"fullName\":\"With Region\",\"email\":\"with.region@logicore.local\","
                + "\"role\":\"OPS_MANAGER\",\"password\":\"" + PASSWORD + "\",\"regionId\":" + south + "}";
        mockMvc.perform(post("/api/users").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(withRegion))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.regionName").value("South India"));
    }
}
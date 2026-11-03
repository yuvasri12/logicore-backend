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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The paged search: totals, slices, filters, the user's scope, and the hold and customs flags. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:logicore_paging_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class ShipmentPagingTest {

    private static final String PASSWORD = "ChangeMe@123";
    private static final String ADMIN = "yuvasris531@gmail.com";
    private static final String OPS_SOUTH = "priya.nair@logicore.com";
    private static final String OPS_UAE = "omar.haddad@logicore.com";
    private static final String STAFF_CHENNAI = "karthik.raja@logicore.com";

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

    private Integer createShipment(String opsToken, String customer, String mode,
                                   String fromCode, String toCode) throws Exception {
        Long from = warehouses.findByCodeIgnoreCase(fromCode).get().getId();
        Long to = warehouses.findByCodeIgnoreCase(toCode).get().getId();
        String body = "{\"customerName\":\"" + customer + "\",\"origin\":\"A\",\"destination\":\"B\","
                + "\"originWarehouseId\":" + from + ",\"destinationWarehouseId\":" + to + ","
                + "\"mode\":\"" + mode + "\",\"weight\":10}";
        MvcResult result = mockMvc.perform(post("/api/shipments")
                        .header("Authorization", "Bearer " + opsToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is2xxSuccessful()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private void receiveAndAssign(String staffToken, Integer id, String condition) throws Exception {
        String receive = "{\"shipmentId\":" + id + ",\"condition\":\"" + condition + "\","
                + "\"weightChecked\":10,\"receivedBy\":\"x\"}";
        mockMvc.perform(post("/api/warehouse/receive")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON).content(receive))
                .andExpect(status().is2xxSuccessful());

        String assign = "{\"shipmentId\":" + id + ",\"zone\":\"P\",\"rack\":\"1\",\"bin\":\"1\",\"assignedBy\":\"x\"}";
        mockMvc.perform(post("/api/warehouse/assign-location")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON).content(assign))
                .andExpect(status().is2xxSuccessful());
    }

    private MockHttpServletRequestBuilder search(String token, String text) {
        return get("/api/shipments/search")
                .header("Authorization", "Bearer " + token)
                .param("q", text);
    }

    @Test
    void pagesReportTotalsAndSlices() throws Exception {
        String ops = token(OPS_SOUTH);
        for (int i = 0; i < 3; i++) {
            createShipment(ops, "PagingTest", "AIR", "CHN-01", "CHN-01");
        }

        mockMvc.perform(search(ops, "PagingTest").param("size", "2").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2));

        mockMvc.perform(search(ops, "PagingTest").param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void searchOnlyReturnsWhatTheUserMaySee() throws Exception {
        String ops = token(OPS_SOUTH);
        createShipment(ops, "ScopeProbe", "AIR", "CHN-01", "CHN-01");

        mockMvc.perform(search(ops, "ScopeProbe"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(search(token(ADMIN), "ScopeProbe"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(search(token(STAFF_CHENNAI), "ScopeProbe"))
                .andExpect(jsonPath("$.totalElements").value(1));

        // A manager from another region does not see it.
        mockMvc.perform(search(token(OPS_UAE), "ScopeProbe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void modeAndStatusFiltersWork() throws Exception {
        String ops = token(OPS_SOUTH);
        createShipment(ops, "FilterProbe", "AIR", "CHN-01", "CHN-01");
        createShipment(ops, "FilterProbe", "OCEAN", "CHN-01", "CHN-01");

        mockMvc.perform(search(ops, "FilterProbe"))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(search(ops, "FilterProbe").param("mode", "OCEAN"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].mode").value("OCEAN"));
        mockMvc.perform(search(ops, "FilterProbe").param("status", "NEW"))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(search(ops, "FilterProbe").param("status", "DELIVERED"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void pageRowsCarryTheHoldFlag() throws Exception {
        String ops = token(OPS_SOUTH);
        Integer id = createShipment(ops, "FlagHold", "AIR", "CHN-01", "CHN-01");
        receiveAndAssign(token(STAFF_CHENNAI), id, "DAMAGED");

        mockMvc.perform(search(ops, "FlagHold"))
                .andExpect(jsonPath("$.content[0].onHold").value(true))
                .andExpect(jsonPath("$.content[0].customsStatus").value("NOT_REQUIRED"));
    }

    @Test
    void pageRowsCarryTheCustomsStatus() throws Exception {
        String ops = token(OPS_SOUTH);
        createShipment(ops, "FlagCustoms", "AIR", "CHN-01", "DXB-01");

        mockMvc.perform(search(ops, "FlagCustoms"))
                .andExpect(jsonPath("$.content[0].customsStatus").value("PENDING"))
                .andExpect(jsonPath("$.content[0].onHold").value(false));
    }

    @Test
    void pageSizeIsCappedAndUnknownSortColumnsAreIgnored() throws Exception {
        String ops = token(OPS_SOUTH);

        mockMvc.perform(search(ops, "").param("size", "500").param("sortBy", "password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }
}
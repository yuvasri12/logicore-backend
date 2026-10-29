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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:logicore_link_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class ShipmentCustomerLinkTest {

    private static final String PASSWORD = "ChangeMe@123";

    // If your ShipmentMode enum has no AIR value, change this to one that exists (see ShipmentMode.java).
    private static final String MODE = "AIR";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WarehouseRepository warehouses;

    private String token() throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", "priya.nair@logicore.com", PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private int createCustomer(String token, String name) throws Exception {
        String body = String.format("{\"name\":\"%s\",\"contactPerson\":\"Asha\",\"email\":\"ops@acme.com\","
                + "\"phone\":\"+91 90000 00000\",\"address\":\"Chennai\"}", name);
        MvcResult result = mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private org.springframework.test.web.servlet.ResultActions createShipment(String token, String customerPart) throws Exception {
        // The Ops Manager works in South India, so the shipment starts at the Chennai hub.
        Long chennai = warehouses.findByCodeIgnoreCase("CHN-01").get().getId();
        String body = "{" + customerPart + "\"origin\":\"Chennai\",\"destination\":\"Mumbai\","
                + "\"originWarehouseId\":" + chennai + ","
                + "\"mode\":\"" + MODE + "\",\"weight\":12.5}";
        return mockMvc.perform(post("/api/shipments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void shipmentTakesNameAndIdFromSavedCustomer() throws Exception {
        String token = token();
        int id = createCustomer(token, "Linked Traders");

        createShipment(token, "\"customerId\":" + id + ",")
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.customerId").value(id))
                .andExpect(jsonPath("$.customerName").value("Linked Traders"));
    }

    @Test
    void inactiveCustomerCannotBePickedForNewShipment() throws Exception {
        String token = token();
        int id = createCustomer(token, "Dormant Exports");

        mockMvc.perform(post("/api/customers/" + id + "/deactivate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is2xxSuccessful());

        createShipment(token, "\"customerId\":" + id + ",")
                .andExpect(status().isConflict());
    }

    @Test
    void unknownCustomerIdGivesNotFound() throws Exception {
        createShipment(token(), "\"customerId\":999999,")
                .andExpect(status().isNotFound());
    }

    @Test
    void shipmentWithoutAnyCustomerIsRejected() throws Exception {
        createShipment(token(), "")
                .andExpect(status().isBadRequest());
    }

    @Test
    void typedCustomerNameStillWorks() throws Exception {
        createShipment(token(), "\"customerName\":\"Walk-in Customer\",")
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.customerName").value("Walk-in Customer"))
                .andExpect(jsonPath("$.customerId").doesNotExist());
    }
}
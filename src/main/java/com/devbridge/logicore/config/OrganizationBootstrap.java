package com.devbridge.logicore.config;

import com.devbridge.logicore.model.AppUser;
import com.devbridge.logicore.model.Region;
import com.devbridge.logicore.model.Shipment;
import com.devbridge.logicore.model.Warehouse;
import com.devbridge.logicore.repository.AppUserRepository;
import com.devbridge.logicore.repository.RegionRepository;
import com.devbridge.logicore.repository.ShipmentRepository;
import com.devbridge.logicore.repository.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Demo locations and assignments, only when app.bootstrap.demo-users is true.
 * Safe to run on every start: it creates what is missing and never overwrites an assignment.
 */
@Component
@Order(2)
public class OrganizationBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OrganizationBootstrap.class);

    private final RegionRepository regions;
    private final WarehouseRepository warehouses;
    private final AppUserRepository users;
    private final ShipmentRepository shipments;
    private final boolean demoUsers;

    public OrganizationBootstrap(RegionRepository regions,
                                 WarehouseRepository warehouses,
                                 AppUserRepository users,
                                 ShipmentRepository shipments,
                                 @Value("${app.bootstrap.demo-users:false}") boolean demoUsers) {
        this.regions = regions;
        this.warehouses = warehouses;
        this.users = users;
        this.shipments = shipments;
        this.demoUsers = demoUsers;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!demoUsers) {
            return;
        }

        Region southIndia = region("SI", "South India");
        Region northIndia = region("NI", "North India");
        Region uae = region("UAE", "United Arab Emirates");

        Warehouse chennai = warehouse("CHN-01", "Chennai Hub", "Chennai", "India", southIndia);
        Warehouse bengaluru = warehouse("BLR-01", "Bengaluru Hub", "Bengaluru", "India", southIndia);
        Warehouse delhi = warehouse("DEL-01", "Delhi Hub", "Delhi", "India", northIndia);
        Warehouse dubai = warehouse("DXB-01", "Dubai Hub", "Dubai", "UAE", uae);

        assignRegion("priya.nair@logicore.com", southIndia);
        assignWarehouse("karthik.raja@logicore.com", chennai);
        assignWarehouse("meena.s@logicore.com", chennai);
        assignRegion("omar.haddad@logicore.com", uae);
        assignWarehouse("faisal.khan@logicore.com", dubai);

        List<Warehouse> all = new ArrayList<Warehouse>();
        all.add(chennai);
        all.add(bengaluru);
        all.add(delhi);
        all.add(dubai);
        placeOldShipments(all, chennai);
    }

    private Region region(String code, String name) {
        Region existing = regions.findByCodeIgnoreCase(code).orElse(null);
        if (existing != null) {
            return existing;
        }
        log.info("Demo region created: {}", code);
        return regions.save(new Region(code, name));
    }

    private Warehouse warehouse(String code, String name, String city, String country, Region region) {
        Warehouse existing = warehouses.findByCodeIgnoreCase(code).orElse(null);
        if (existing != null) {
            return existing;
        }
        log.info("Demo warehouse created: {}", code);
        return warehouses.save(new Warehouse(code, name, city, country, region));
    }

    private void assignRegion(String email, Region region) {
        AppUser user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user != null && user.getRegion() == null && user.getWarehouse() == null) {
            user.setRegion(region);
            users.save(user);
            log.info("Demo user assigned to region {}: userId={}", region.getCode(), user.getId());
        }
    }

    private void assignWarehouse(String email, Warehouse warehouse) {
        AppUser user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user != null && user.getRegion() == null && user.getWarehouse() == null) {
            user.setWarehouse(warehouse);
            users.save(user);
            log.info("Demo user assigned to warehouse {}: userId={}", warehouse.getCode(), user.getId());
        }
    }

    /**
     * Shipments made before locations existed have no warehouse. Match the city in the origin
     * and destination text; an origin that matches nothing goes to the default warehouse.
     */
    private void placeOldShipments(List<Warehouse> all, Warehouse defaultOrigin) {
        int updated = 0;
        for (Shipment shipment : shipments.findAll()) {
            boolean changed = false;
            if (shipment.getOriginWarehouse() == null) {
                Warehouse match = match(all, shipment.getOrigin());
                shipment.setOriginWarehouse(match != null ? match : defaultOrigin);
                changed = true;
            }
            if (shipment.getDestinationWarehouse() == null) {
                Warehouse match = match(all, shipment.getDestination());
                if (match != null) {
                    shipment.setDestinationWarehouse(match);
                    changed = true;
                }
            }
            if (changed) {
                shipments.save(shipment);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("Demo data: {} old shipments placed in warehouses", updated);
        }
    }

    private Warehouse match(List<Warehouse> all, String text) {
        if (text == null) {
            return null;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (Warehouse warehouse : all) {
            if (warehouse.getCity() != null && lower.contains(warehouse.getCity().toLowerCase(Locale.ROOT))) {
                return warehouse;
            }
        }
        return null;
    }
}
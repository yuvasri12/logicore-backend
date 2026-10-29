package com.devbridge.logicore.controller;

import com.devbridge.logicore.dto.RegionRequest;
import com.devbridge.logicore.dto.RegionResponse;
import com.devbridge.logicore.dto.WarehouseRequest;
import com.devbridge.logicore.dto.WarehouseResponse;
import com.devbridge.logicore.service.OrganizationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/** Regions and warehouses. Reading: admin and ops manager. Changing: admin only (see SecurityConfig). */
@RestController
@RequestMapping("/api/org")
public class OrganizationController {

    private final OrganizationService organization;

    public OrganizationController(OrganizationService organization) {
        this.organization = organization;
    }

    @GetMapping("/regions")
    public List<RegionResponse> listRegions() {
        return organization.listRegions();
    }

    @PostMapping("/regions")
    @ResponseStatus(HttpStatus.CREATED)
    public RegionResponse createRegion(@Valid @RequestBody RegionRequest request) {
        return organization.createRegion(request);
    }

    @PutMapping("/regions/{id}")
    public RegionResponse updateRegion(@PathVariable Long id, @Valid @RequestBody RegionRequest request) {
        return organization.updateRegion(id, request);
    }

    @GetMapping("/warehouses")
    public List<WarehouseResponse> listWarehouses(@RequestParam(required = false) Long regionId) {
        return organization.listWarehouses(regionId);
    }

    @PostMapping("/warehouses")
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseResponse createWarehouse(@Valid @RequestBody WarehouseRequest request) {
        return organization.createWarehouse(request);
    }

    @PutMapping("/warehouses/{id}")
    public WarehouseResponse updateWarehouse(@PathVariable Long id, @Valid @RequestBody WarehouseRequest request) {
        return organization.updateWarehouse(id, request);
    }
}
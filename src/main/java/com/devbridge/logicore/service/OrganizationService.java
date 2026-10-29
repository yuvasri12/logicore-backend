package com.devbridge.logicore.service;

import com.devbridge.logicore.dto.RegionRequest;
import com.devbridge.logicore.dto.RegionResponse;
import com.devbridge.logicore.dto.WarehouseRequest;
import com.devbridge.logicore.dto.WarehouseResponse;
import com.devbridge.logicore.enums.AuditAction;
import com.devbridge.logicore.enums.ErrorCode;
import com.devbridge.logicore.exception.BusinessException;
import com.devbridge.logicore.exception.ResourceNotFoundException;
import com.devbridge.logicore.model.Region;
import com.devbridge.logicore.model.Warehouse;
import com.devbridge.logicore.repository.RegionRepository;
import com.devbridge.logicore.repository.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Regions and warehouses: the company's locations. Admin manages them. */
@Service
public class OrganizationService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);
    private static final String REGION = "REGION";
    private static final String WAREHOUSE = "WAREHOUSE";

    private final RegionRepository regions;
    private final WarehouseRepository warehouses;
    private final AuditService audit;

    public OrganizationService(RegionRepository regions, WarehouseRepository warehouses, AuditService audit) {
        this.regions = regions;
        this.warehouses = warehouses;
        this.audit = audit;
    }

    // ---------- regions ----------

    @Transactional(readOnly = true)
    public List<RegionResponse> listRegions() {
        List<RegionResponse> result = new ArrayList<RegionResponse>();
        for (Region region : regions.findAllByOrderByNameAsc()) {
            result.add(toResponse(region));
        }
        return result;
    }

    @Transactional
    public RegionResponse createRegion(RegionRequest request) {
        String code = normalizeCode(request.getCode());
        if (regions.existsByCodeIgnoreCase(code)) {
            throw new BusinessException(ErrorCode.REGION_CODE_EXISTS);
        }
        Region region = new Region(code, request.getName().trim());
        if (request.getActive() != null) {
            region.setActive(request.getActive().booleanValue());
        }
        Region saved = regions.save(region);
        audit.record(AuditAction.REGION_CREATED, REGION, saved.getId(),
                "code=" + saved.getCode() + ", name=" + saved.getName());
        log.info("Region created: id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public RegionResponse updateRegion(Long id, RegionRequest request) {
        Region region = regions.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.REGION_NOT_FOUND, id));

        String code = normalizeCode(request.getCode());
        Optional<Region> sameCode = regions.findByCodeIgnoreCase(code);
        if (sameCode.isPresent() && !sameCode.get().getId().equals(id)) {
            throw new BusinessException(ErrorCode.REGION_CODE_EXISTS);
        }

        region.setCode(code);
        region.setName(request.getName().trim());
        if (request.getActive() != null) {
            region.setActive(request.getActive().booleanValue());
        }
        Region saved = regions.save(region);
        audit.record(AuditAction.REGION_UPDATED, REGION, saved.getId(),
                "code=" + saved.getCode() + ", name=" + saved.getName() + ", active=" + saved.isActive());
        log.info("Region updated: id={}", saved.getId());
        return toResponse(saved);
    }

    // ---------- warehouses ----------

    /** All warehouses, or only those of one region when regionId is given. */
    @Transactional(readOnly = true)
    public List<WarehouseResponse> listWarehouses(Long regionId) {
        List<Warehouse> found = regionId == null
                ? warehouses.findAllByOrderByNameAsc()
                : warehouses.findByRegionIdOrderByNameAsc(regionId);
        List<WarehouseResponse> result = new ArrayList<WarehouseResponse>();
        for (Warehouse warehouse : found) {
            result.add(toResponse(warehouse));
        }
        return result;
    }

    @Transactional
    public WarehouseResponse createWarehouse(WarehouseRequest request) {
        String code = normalizeCode(request.getCode());
        if (warehouses.existsByCodeIgnoreCase(code)) {
            throw new BusinessException(ErrorCode.WAREHOUSE_CODE_EXISTS);
        }
        Region region = findRegion(request.getRegionId());

        Warehouse warehouse = new Warehouse(code, request.getName().trim(),
                clean(request.getCity()), clean(request.getCountry()), region);
        if (request.getActive() != null) {
            warehouse.setActive(request.getActive().booleanValue());
        }
        Warehouse saved = warehouses.save(warehouse);
        audit.record(AuditAction.WAREHOUSE_CREATED, WAREHOUSE, saved.getId(),
                "code=" + saved.getCode() + ", region=" + region.getCode());
        log.info("Warehouse created: id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public WarehouseResponse updateWarehouse(Long id, WarehouseRequest request) {
        Warehouse warehouse = warehouses.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.WAREHOUSE_NOT_FOUND, id));

        String code = normalizeCode(request.getCode());
        Optional<Warehouse> sameCode = warehouses.findByCodeIgnoreCase(code);
        if (sameCode.isPresent() && !sameCode.get().getId().equals(id)) {
            throw new BusinessException(ErrorCode.WAREHOUSE_CODE_EXISTS);
        }
        Region region = findRegion(request.getRegionId());

        warehouse.setCode(code);
        warehouse.setName(request.getName().trim());
        warehouse.setCity(clean(request.getCity()));
        warehouse.setCountry(clean(request.getCountry()));
        warehouse.setRegion(region);
        if (request.getActive() != null) {
            warehouse.setActive(request.getActive().booleanValue());
        }
        Warehouse saved = warehouses.save(warehouse);
        audit.record(AuditAction.WAREHOUSE_UPDATED, WAREHOUSE, saved.getId(),
                "code=" + saved.getCode() + ", region=" + region.getCode() + ", active=" + saved.isActive());
        log.info("Warehouse updated: id={}", saved.getId());
        return toResponse(saved);
    }

    // ---------- helpers ----------

    private Region findRegion(Long id) {
        return regions.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.REGION_NOT_FOUND, id));
    }

    /** Codes are stored trimmed and in upper case, so "sin" and "SIN" are the same code. */
    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    /** Trims text; an empty value is stored as null. */
    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private RegionResponse toResponse(Region region) {
        return new RegionResponse(region.getId(), region.getCode(), region.getName(), region.isActive());
    }

    private WarehouseResponse toResponse(Warehouse warehouse) {
        Region region = warehouse.getRegion();
        return new WarehouseResponse(warehouse.getId(), warehouse.getCode(), warehouse.getName(),
                warehouse.getCity(), warehouse.getCountry(),
                region.getId(), region.getName(), warehouse.isActive());
    }
}
package com.devbridge.logicore.controller;

import com.devbridge.logicore.dto.*;
import com.devbridge.logicore.service.WarehouseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @PostMapping("/warehouse/receive")
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseReceiptResponse receive(@Valid @RequestBody WarehouseReceiveRequest request) {
        return warehouseService.receive(request);
    }

    @PostMapping("/warehouse/assign-location")
    @ResponseStatus(HttpStatus.CREATED)
    public FreightAssignmentResponse assignLocation(@Valid @RequestBody AssignLocationRequest request) {
        return warehouseService.assignLocation(request);
    }

    @PostMapping("/dispatch")
    @ResponseStatus(HttpStatus.CREATED)
    public DispatchResponse dispatch(@Valid @RequestBody DispatchRequest request) {
        return warehouseService.dispatch(request);
    }
}

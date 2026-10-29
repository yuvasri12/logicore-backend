package com.devbridge.logicore.controller;

import com.devbridge.logicore.dto.*;
import com.devbridge.logicore.service.ReceiptHoldService;
import com.devbridge.logicore.service.WarehouseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api")
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final ReceiptHoldService receiptHoldService;

    public WarehouseController(WarehouseService warehouseService, ReceiptHoldService receiptHoldService) {
        this.warehouseService = warehouseService;
        this.receiptHoldService = receiptHoldService;
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

    /** Ops Manager or Admin accepts a receiving problem so the shipment can move on. */
    @PostMapping("/warehouse/shipments/{shipmentId}/clear-issue")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearIssue(@PathVariable Long shipmentId, @Valid @RequestBody ClearIssueRequest request) {
        receiptHoldService.clearIssue(shipmentId, request.getNote());
    }

    @PostMapping("/dispatch")
    @ResponseStatus(HttpStatus.CREATED)
    public DispatchResponse dispatch(@Valid @RequestBody DispatchRequest request) {
        return warehouseService.dispatch(request);
    }
}
package com.devbridge.logicore.controller;

import com.devbridge.logicore.dto.*;
import com.devbridge.logicore.enums.ShipmentMode;
import com.devbridge.logicore.enums.ShipmentStatus;
import com.devbridge.logicore.service.ShipmentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentResponse createShipment(@Valid @RequestBody CreateShipmentRequest request) {
        return shipmentService.createShipment(request);
    }

    @GetMapping
    public List<ShipmentResponse> listShipments(
            @RequestParam(required = false) ShipmentStatus status,
            @RequestParam(required = false) ShipmentMode mode,
            @RequestParam(required = false) String customerName) {
        return shipmentService.listShipments(status, mode, customerName);
    }

    @GetMapping("/{id}")
    public ShipmentResponse getShipment(@PathVariable Long id) {
        return shipmentService.getShipment(id);
    }

    @PutMapping("/{id}")
    public ShipmentResponse updateShipment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShipmentRequest request) {
        return shipmentService.updateShipment(id, request);
    }

    @PatchMapping("/{id}/status")
    public ShipmentResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return shipmentService.updateStatus(id, request);
    }

    @PostMapping("/{id}/load")
    public ShipmentResponse markLoaded(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentActionRequest request) {
        return shipmentService.markLoaded(id, request);
    }

    @PostMapping("/{id}/deliver")
    public ShipmentResponse markDelivered(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentActionRequest request) {
        return shipmentService.markDelivered(id, request);
    }

    @DeleteMapping("/{id}")
    public ShipmentResponse cancelShipment(
            @PathVariable Long id,
            @Valid @RequestBody CancelShipmentRequest request) {
        return shipmentService.cancelShipment(id, request);
    }
}